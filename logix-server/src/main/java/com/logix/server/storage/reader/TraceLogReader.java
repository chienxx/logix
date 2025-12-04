package com.logix.server.storage.reader;

import com.clickhouse.client.ClickHouseClient;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.ClickHouseNodes;
import com.clickhouse.client.ClickHouseResponse;
import com.clickhouse.data.ClickHouseFormat;
import com.clickhouse.data.ClickHouseRecord;
import com.logix.server.model.logging.TraceQueryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 链路日志查询
 *
 * @author Kanade
 * @since 2025/11/23
 */
@Slf4j
@Component
public class TraceLogReader {

    private final ClickHouseClient client;
    private final ClickHouseNodes nodes;
    private final String tableName;

    public TraceLogReader(ClickHouseClient client, ClickHouseNodes nodes,
                          @Value("${logix.clickhouse.db-name}") String database) {
        this.client = client;
        this.nodes = nodes;
        this.tableName = database + ".trace_logs";
        log.info("[TraceLogReader] 初始化完成 | 查询表: {}", tableName);
    }

    /**
     * 查询链路追踪原始数据
     */
    public List<TraceRow> queryByTraceId(TraceQueryRequest request) {
        String sql = buildSql(request);

        long start = System.nanoTime();
        try {
            List<TraceRow> results = new ArrayList<>();
            ClickHouseNode node = nodes.apply(nodes.getNodeSelector());
            try (ClickHouseResponse response = client.read(node)
                    .query(sql)
                    .format(ClickHouseFormat.RowBinaryWithNamesAndTypes)
                    .executeAndWait()) {
                for (ClickHouseRecord record : response.records()) {
                    results.add(mapToTraceRow(record));
                }
            }
            return results;
        } catch (Exception e) {
            log.error("[TraceLogReader] 查询失败 | traceId:{}", request.getTraceId(), e);
            throw new RuntimeException("查询链路失败: " + e.getMessage(), e);
        }
    }

    private String buildSql(TraceQueryRequest request) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT trace_id, method_name, toUnixTimestamp64Milli(event_time) as dt, ");
        sql.append("app_name, server_ip, position, depth FROM ").append(tableName);
        sql.append(" WHERE trace_id = '").append(escape(request.getTraceId())).append("'");

        if (StringUtils.hasText(request.getAppName())) {
            sql.append(" AND app_name = '").append(escape(request.getAppName())).append("'");
        }
        if (request.getStartTime() != null) {
            sql.append(" AND event_time >= fromUnixTimestamp64Milli(").append(request.getStartTime()).append(")");
        }
        if (request.getEndTime() != null) {
            sql.append(" AND event_time <= fromUnixTimestamp64Milli(").append(request.getEndTime()).append(")");
        }
        sql.append(" ORDER BY event_time ASC");
        return sql.toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private TraceRow mapToTraceRow(ClickHouseRecord r) {
        TraceRow row = new TraceRow();
        row.traceId = r.getValue(0).asString();
        row.methodName = r.getValue(1).asString();
        row.dt = r.getValue(2).asLong();
        row.appName = r.getValue(3).asString();
        row.serverIp = r.getValue(4).asString();
        // position 是 Enum8，getValue 返回的是整数
        int posVal = r.getValue(5).asInteger();
        row.position = posVal == 1 ? "<" : (posVal == 2 ? ">" : String.valueOf(posVal));
        row.depth = r.getValue(6).asInteger();
        return row;
    }

    /**
     * 链路查询原始行数据
     */
    public static class TraceRow {
        public String traceId;
        public String methodName;
        public Long dt;
        public String appName;
        public String serverIp;
        public String position;
        public Integer depth;
    }
}
