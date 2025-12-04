package com.logix.client.core.logging;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 日志通道，封装队列缓冲与批量触发策略
 *
 * @author Kanade
 * @since 2025/10/24
 */
@Slf4j
final class LogQueueChannel {

    @FunctionalInterface
    interface Consumer {
        void accept(List<String> payload);
    }

    private volatile boolean running = true;

    private final String name;
    private final BlockingQueue<String> queue;
    private final int maxBatch;
    private final long batchTimeout;
    private final AtomicLong lastFlushTime = new AtomicLong();

    LogQueueChannel(String name, int capacity, int maxBatch, long batchTimeout) {
        this.name = name;
        this.maxBatch = maxBatch;
        this.batchTimeout = batchTimeout;
        this.queue = new LinkedBlockingQueue<>(capacity);
    }

    void publish(String logEvent) {
        if (logEvent == null) return;

        if (!queue.offer(logEvent)) {
            // 每 10 秒最多打印一次
            if (System.currentTimeMillis() % 10000 < 10) {
                log.debug("[{}] queue full, dropping logs", name);
            }
        }
    }

    void consume(Consumer consumer) {
        List<String> logEvents = new ArrayList<>(maxBatch);
        while (running) {
            try {
                logEvents.clear();

                int size = queue.size();
                long now = System.currentTimeMillis();
                long elapsed = now - lastFlushTime.get();

                if (size >= maxBatch || elapsed > batchTimeout) {
                    queue.drainTo(logEvents, maxBatch);
                } else if (size == 0) {
                    //队列为空，阻塞等待
                    String log = queue.take();
                    logEvents.add(log);
                    //尝试获取更多
                    queue.drainTo(logEvents, maxBatch - 1);
                } else {
                    TimeUnit.MILLISECONDS.sleep(100);
                }

                // 统一发送和更新时间
                if (!logEvents.isEmpty()) {
                    consumer.accept(logEvents);
                }
                lastFlushTime.set(now);
            } catch (Exception e) {
                pauseOnError();
            }
        }
    }

    void stop() {
        running = false;
    }

    private void pauseOnError() {
        try {
            TimeUnit.MILLISECONDS.sleep(1000);
        } catch (InterruptedException ignore) {
        }
    }
}
