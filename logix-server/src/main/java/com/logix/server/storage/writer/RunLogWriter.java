package com.logix.server.storage.writer;

import com.clickhouse.client.*;
import com.clickhouse.data.ClickHouseFormat;
import com.logix.common.enums.LogLevel;
import com.logix.common.model.RunLogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * RunLog ClickHouse写入器
 *
 * @author Kanade
 * @since 2025/11/23
 */
@Slf4j
@Component
public class RunLogWriter implements ClickHouseWriter<RunLogEvent> {

    private static final int ESTIMATED_ROW_BYTES = 512;
    private static final int[] LOG_LEVEL_MAP = {1, 2, 3, 4, 5}; // TRACE, DEBUG, INFO, WARN, ERROR

    private final ClickHouseClient client;
    private final ClickHouseNodes nodes;
    private final String insertSql;

    public RunLogWriter(ClickHouseClient client, ClickHouseNodes nodes,
                        @Value("${logix.clickhouse.db-name}") String database) {
        this.client = client;
        this.nodes = nodes;
        this.insertSql = buildInsertSql(database + ".run_logs");
        log.info("[RunLogWriter] 初始化完成 | 目标表: {}.run_logs", database);
    }

    private static String buildInsertSql(String tableName) {
        return "INSERT INTO " + tableName + " (event_time, app_name, env, server_ip, seq, " +
                "log_level, content, class_name, method_name, thread_name, trace_id) " +
                "SETTINGS async_insert=1, wait_for_async_insert=0 FORMAT RowBinary";
    }

    @Override
    public void batchInsert(List<RunLogEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        long start = System.nanoTime();
        try {
            byte[] payload = serialize(events);
            executeInsert(payload);
            log.info("[RunLogWriter] 批量写入成功 | size:{} | cost:{}ms",
                    events.size(), (System.nanoTime() - start) / 1_000_000);
        } catch (Exception e) {
            log.error("[RunLogWriter] 批量写入失败 | size:{}", events.size(), e);
        }
    }

    private byte[] serialize(List<RunLogEvent> events) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(ESTIMATED_ROW_BYTES * events.size());
             DataOutputStream out = new DataOutputStream(baos)) {
            RowBinaryEncoder encoder = new RowBinaryEncoder(out);
            for (RunLogEvent event : events) {
                encoder.writeDateTime64Millis(event.getEventTime());
                encoder.writeString(event.getAppName());
                encoder.writeString(event.getEnv());
                encoder.writeString(event.getServerIp());
                encoder.writeUInt64(event.getSeq());
                encoder.writeEnum8(mapLogLevel(event.getLogLevel()));
                encoder.writeString(event.getContent());
                encoder.writeString(event.getClassName());
                encoder.writeString(event.getMethodName());
                encoder.writeString(event.getThreadName());
                encoder.writeString(event.getTraceId());
            }
            out.flush();
            return baos.toByteArray();
        }
    }

    private void executeInsert(byte[] payload) throws Exception {
        ClickHouseNode node = nodes.apply(nodes.getNodeSelector());
        try (ClickHouseResponse ignored = client.read(node)
                .write()
                .format(ClickHouseFormat.RowBinary)
                .query(insertSql)
                .data(new ByteArrayInputStream(payload))
                .executeAndWait()) {
            // success
        }
    }

    private int mapLogLevel(LogLevel level) {
        return level != null ? LOG_LEVEL_MAP[level.ordinal()] : 3;
    }
}
