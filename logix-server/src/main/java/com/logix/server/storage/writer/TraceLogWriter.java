package com.logix.server.storage.writer;

import com.clickhouse.client.*;
import com.clickhouse.data.ClickHouseFormat;
import com.logix.common.model.TraceLogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * TraceLog ClickHouse写入器
 *
 * @author Kanade
 * @since 2025/11/23
 */
@Slf4j
@Component
public class TraceLogWriter implements ClickHouseWriter<TraceLogEvent> {

    private static final int ESTIMATED_ROW_BYTES = 256;

    private final ClickHouseClient client;
    private final ClickHouseNodes nodes;
    private final String insertSql;

    public TraceLogWriter(ClickHouseClient client, ClickHouseNodes nodes,
                          @Value("${logix.clickhouse.db-name}") String database) {
        this.client = client;
        this.nodes = nodes;
        this.insertSql = buildInsertSql(database + ".trace_logs");
        log.info("[TraceLogWriter] 初始化完成 | 目标表: {}.trace_logs", database);
    }

    private static String buildInsertSql(String tableName) {
        return "INSERT INTO " + tableName + " (event_time, app_name, env, server_ip, " +
                "trace_id, method_name, position, depth) " +
                "SETTINGS async_insert=1, wait_for_async_insert=0 FORMAT RowBinary";
    }

    @Override
    public void batchInsert(List<TraceLogEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        long start = System.nanoTime();
        try {
            byte[] payload = serialize(events);
            executeInsert(payload);
            log.info("[TraceLogWriter] 批量写入成功 | size:{} | cost:{}ms",
                    events.size(), (System.nanoTime() - start) / 1_000_000);
        } catch (Exception e) {
            log.error("[TraceLogWriter] 批量写入失败 | size:{}", events.size(), e);
        }
    }

    private byte[] serialize(List<TraceLogEvent> events) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(ESTIMATED_ROW_BYTES * events.size());
             DataOutputStream out = new DataOutputStream(baos)) {
            RowBinaryEncoder encoder = new RowBinaryEncoder(out);
            for (TraceLogEvent event : events) {
                encoder.writeDateTime64Millis(event.getEventTime());
                encoder.writeString(event.getAppName());
                encoder.writeString(event.getEnv());
                encoder.writeString(event.getServerIp());
                encoder.writeString(event.getTraceId());
                encoder.writeString(event.getMethodName());
                encoder.writeEnum8(mapPosition(event.getPosition()));
                encoder.writeUInt16(event.getDepth());
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

    private int mapPosition(String position) {
        if ("<".equals(position)) return 1;  // START
        if (">".equals(position)) return 2;  // END
        return 0;
    }
}
