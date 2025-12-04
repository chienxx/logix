package com.logix.server.consumer;

import com.logix.common.model.BaseLogEvent;
import com.logix.server.storage.writer.ClickHouseWriter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 定期从队列取出数据，批量写入 ClickHouse
 *
 * @author Kanade
 * @since 2025/11/20
 */
@Slf4j
public class LogBatchWriter<T extends BaseLogEvent> implements Runnable {

    private volatile boolean running = true;

    private final String logType;
    private final BlockingQueue<T> queue;
    private final ClickHouseWriter<T> writer;
    private final int batchSize;
    private final int batchTimeoutMs;

    public LogBatchWriter(
            String logType,
            BlockingQueue<T> queue,
            ClickHouseWriter<T> writer,
            int batchSize,
            int batchTimeoutMs) {
        this.logType = logType;
        this.queue = queue;
        this.writer = writer;
        this.batchSize = batchSize;
        this.batchTimeoutMs = batchTimeoutMs;
    }

    @Override
    public void run() {
        log.info("[{}LogWriter] 批量写入线程启动 | batchSize:{} | timeout:{}ms",
            logType, batchSize, batchTimeoutMs);

        while (running) {
            try {
                List<T> events = new ArrayList<>(batchSize);

                // 阻塞等待第一条消息
                T first = queue.poll(batchTimeoutMs, TimeUnit.MILLISECONDS);
                if (first != null) {
                    events.add(first);
                    // 尝试批量获取更多消息
                    queue.drainTo(events, batchSize - 1);
                }

                if (!events.isEmpty()) {
                    writer.batchInsert(events);
                }
            } catch (Exception e) {
                log.error("[{}Writer] 批量写入异常", logType, e);
            }
        }

        // 关闭时清空队列
        drainRemaining();
        log.info("[{}Writer] 批量写入线程已停止", logType);
    }

    public void stop() {
        running = false;
    }

    private void drainRemaining() {
        List<T> remaining = new ArrayList<>();
        queue.drainTo(remaining);

        if (!remaining.isEmpty()) {
            log.info("[{}Writer] 清空队列，剩余消息: {} 条", logType, remaining.size());
            try {
                writer.batchInsert(remaining);
            } catch (Exception e) {
                log.error("[{}Writer] 清空队列时写入失败", logType, e);
            }
        }
    }
}