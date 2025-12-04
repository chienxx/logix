package com.logix.server.consumer;

import com.logix.common.util.ThreadPoolUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Kafka批量消费
 *
 * @author Kanade
 * @since 2025/11/20
 */
@Slf4j
public final class LogBatchConsumer implements AutoCloseable {

    private volatile boolean running = true;

    private final KafkaConsumer<String, String> consumer;
    private final List<String> topics;
    private final String groupId;
    private final LogMessageRouter router;
    private final ExecutorService executor;

    public LogBatchConsumer(
            Properties consumerProps,
            List<String> topics,
            String groupId,
            LogMessageRouter router) {
        this.topics = topics;
        this.groupId = groupId;
        this.router = router;
        this.consumer = new KafkaConsumer<>(consumerProps);
        this.consumer.subscribe(topics);
        this.executor = ThreadPoolUtils.createKafkaConsumerExecutor();
    }

    public void start() {
        executor.execute(this::consume);
        log.info("[Kafka消费者] 已启动 | topics:{} | group:{}", topics, groupId);
    }

    private void consume() {
        try {
            while (running) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

                    if (!records.isEmpty()) {
                        // 路由到不同的队列
                        router.route(records);
                    }
                } catch (Exception e) {
                    log.error("[Kafka消费者] 批量拉取失败", e);
                }
            }
        } finally {
            closeConsumer();
        }
    }

    private void closeConsumer() {
        consumer.close();
    }

    @Override
    public void close() {
        running = false;
        executor.shutdown();
        log.info("日志消费者已停止");
    }
}
