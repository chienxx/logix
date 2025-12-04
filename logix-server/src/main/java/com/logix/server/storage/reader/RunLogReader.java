package com.logix.server.storage.reader;

import com.clickhouse.client.ClickHouseClient;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.ClickHouseNodes;
import com.clickhouse.client.ClickHouseResponse;
import com.clickhouse.data.ClickHouseFormat;
import com.clickhouse.data.ClickHouseRecord;
import com.logix.server.model.logging.LogFilterCriteria;
import com.logix.server.model.logging.LogRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 运行日志查询
 *
 * @author Kanade
 * @since 2025/11/23
 */
@Slf4j
@Component
public class RunLogReader {

    private static final String[] LEVEL_NAMES = {"", "TRACE", "DEBUG", "INFO", "WARN", "ERROR"};

    private final ClickHouseClient client;
    private final ClickHouseNodes nodes;
    private final String tableName;

    public RunLogReader(ClickHouseClient client, ClickHouseNodes nodes,
                        @Value("${logix.clickhouse.db-name}") String database) {
        this.client = client;
        this.nodes = nodes;
        this.tableName = database + ".run_logs";
        log.info("[RunLogReader] 初始化完成 | 查询表: {}", tableName);
    }

    /**
     * 分页查询日志（时间倒序）
     * 用于历史日志查询场景
     */
    public List<LogRecord> queryLogs(LogFilterCriteria criteria, int offset, int limit) {
        String sql = buildSelectSql(criteria, offset, limit, false);
        return executeQuery(sql);
    }

    /**
     * 实时拉取日志（时间正序 + 游标过滤）
     * 用于实时监控场景，从游标位置往后获取新数据
     */
    public List<LogRecord> pollLogs(LogFilterCriteria criteria, int limit) {
        String sql = buildSelectSql(criteria, 0, limit, true);
        return executeQuery(sql);
    }

    private List<LogRecord> executeQuery(String sql) {
        long start = System.nanoTime();
        try {
            List<LogRecord> results = new ArrayList<>();
            ClickHouseNode node = nodes.apply(nodes.getNodeSelector());
            try (ClickHouseResponse response = client.read(node)
                    .query(sql)
                    .format(ClickHouseFormat.RowBinaryWithNamesAndTypes)
                    .executeAndWait()) {
                for (ClickHouseRecord record : response.records()) {
                    results.add(mapToLogRecord(record));
                }
            }
            return results;
        } catch (Exception e) {
            log.error("[RunLogReader] 查询失败", e);
            throw new RuntimeException("查询日志失败: " + e.getMessage(), e);
        }
    }

    public int queryCount(LogFilterCriteria criteria) {
        String sql = "SELECT COUNT(*) FROM " + tableName + buildWhereClause(criteria);
        try {
            ClickHouseNode node = nodes.apply(nodes.getNodeSelector());
            try (ClickHouseResponse response = client.read(node).query(sql).executeAndWait()) {
                for (ClickHouseRecord record : response.records()) {
                    return record.getValue(0).asInteger();
                }
                return 0;
            }
        } catch (Exception e) {
            log.error("[RunLogReader] 查询总数失败", e);
            return 0;
        }
    }

    public List<String> queryAppNames() {
        return queryStringColumn("SELECT DISTINCT app_name FROM " + tableName + " ORDER BY app_name");
    }

    public List<String> queryEnvs() {
        return queryStringColumn("SELECT DISTINCT env FROM " + tableName + " WHERE env != '' ORDER BY env");
    }

    private List<String> queryStringColumn(String sql) {
        try {
            List<String> results = new ArrayList<>();
            ClickHouseNode node = nodes.apply(nodes.getNodeSelector());
            try (ClickHouseResponse response = client.read(node)
                    .query(sql)
                    .format(ClickHouseFormat.RowBinaryWithNamesAndTypes)
                    .executeAndWait()) {
                for (ClickHouseRecord record : response.records()) {
                    results.add(record.getValue(0).asString());
                }
            }
            return results;
        } catch (Exception e) {
            log.error("[RunLogReader] 查询失败: {}", sql, e);
            throw new RuntimeException("查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建查询SQL
     *
     * @param criteria   过滤条件
     * @param offset     偏移量
     * @param limit      限制数量
     * @param isRealtime true=实时拉取（时间正序+游标过滤），false=分页查询（时间倒序）
     */
    private String buildSelectSql(LogFilterCriteria criteria, int offset, int limit, boolean isRealtime) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT seq, log_level, event_time, app_name, env, server_ip, ");
        sql.append("method_name, content, class_name, thread_name, trace_id ");
        sql.append("FROM ").append(tableName);
        sql.append(buildWhereClause(criteria, isRealtime));

        if (isRealtime) {
            // 实时拉取：时间正序，从游标位置往后取新数据
            sql.append(" ORDER BY event_time ASC, seq ASC");
        } else {
            // 分页查询：时间倒序，最新的在前面
            sql.append(" ORDER BY event_time DESC, seq DESC");
        }

        sql.append(" LIMIT ").append(limit);
        if (offset > 0) {
            sql.append(" OFFSET ").append(offset);
        }
        return sql.toString();
    }

    private String buildWhereClause(LogFilterCriteria c) {
        return buildWhereClause(c, false);
    }

    /**
     * 构建WHERE子句
     *
     * @param c          过滤条件
     * @param isRealtime true=实时拉取模式，会增加游标过滤条件
     */
    private String buildWhereClause(LogFilterCriteria c, boolean isRealtime) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        appendEq(where, "app_name", c.getAppName());
        appendEq(where, "env", c.getEnv());
        appendEq(where, "trace_id", c.getTraceId());
        appendEq(where, "server_ip", c.getServerIp());

        if (StringUtils.hasText(c.getLevel())) {
            int level = parseLevelCode(c.getLevel());
            if (level > 0) where.append(" AND log_level = ").append(level);
        }
        if (c.getStartTime() != null) {
            where.append(" AND event_time >= toDateTime64(").append(c.getStartTime()).append(" / 1000, 3)");
        }
        if (c.getEndTime() != null) {
            where.append(" AND event_time <= toDateTime64(").append(c.getEndTime()).append(" / 1000, 3)");
        }
        if (StringUtils.hasText(c.getKeyword())) {
            where.append(" AND positionCaseInsensitive(content, '").append(escape(c.getKeyword())).append("') > 0");
        }

        // 实时拉取模式：游标过滤，取游标之后的数据
        if (isRealtime && c.getCursorDt() != null && c.getCursorSeq() != null) {
            where.append(" AND (event_time, seq) > (toDateTime64(")
                    .append(c.getCursorDt())
                    .append(" / 1000, 3), ")
                    .append(c.getCursorSeq())
                    .append(")");
        }
        return where.toString();
    }

    private void appendEq(StringBuilder where, String column, String value) {
        if (StringUtils.hasText(value)) {
            where.append(" AND ").append(column).append(" = '").append(escape(value)).append("'");
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private int parseLevelCode(String level) {
        for (int i = 1; i < LEVEL_NAMES.length; i++) {
            if (LEVEL_NAMES[i].equalsIgnoreCase(level)) return i;
        }
        return 0;
    }

    private LogRecord mapToLogRecord(ClickHouseRecord r) {
        LogRecord record = new LogRecord();
        record.setSeq(r.getValue(0).asLong());
        int levelCode = r.getValue(1).asInteger();
        record.setLevel(levelCode > 0 && levelCode < LEVEL_NAMES.length ? LEVEL_NAMES[levelCode] : "INFO");
        record.setDt(r.getValue(2).asInstant().toEpochMilli());
        record.setAppName(r.getValue(3).asString());
        record.setEnv(r.getValue(4).asString());
        record.setServerIp(r.getValue(5).asString());
        record.setMethod(r.getValue(6).asString());
        record.setContent(r.getValue(7).asString());
        record.setClassName(r.getValue(8).asString());
        record.setThreadName(r.getValue(9).asString());
        record.setTraceId(r.getValue(10).asString());
        return record;
    }
}
