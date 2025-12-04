package com.logix.client.core.logging;

import com.logix.client.core.circuit.CircuitBreaker;
import com.logix.client.core.kafka.KafkaProducerClient;
import com.logix.common.config.KafkaSecurityConfig;
import com.logix.common.enums.LogType;
import com.logix.common.util.ThreadPoolUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import static com.logix.common.constants.LogixConstants.Kafka.RUN_TOPIC;
import static com.logix.common.constants.LogixConstants.Kafka.TRACE_TOPIC;

/**
 * 日志异步分发器，负责调度运行日志与链路日志的缓冲和投递
 *
 * @author Kanade
 * @since 2025/10/22
 */
@Slf4j
public final class LogEventDispatcher implements AutoCloseable {

    private static final ThreadPoolExecutor threadPoolExecutor = ThreadPoolUtils.createDefaultExecutor();

    private final KafkaProducerClient client;
    private final LogQueueChannel runChannel;
    private final LogQueueChannel traceChannel;
    private final int workerCount;
    private final CircuitBreaker circuitBreaker;

    public LogEventDispatcher(DispatcherConfig config) {
        this.circuitBreaker = new CircuitBreaker(Duration.ofSeconds(config.getQuietPeriod()));
        this.client = KafkaProducerClient.getInstance(config.getBootstrapServers(), config.getSecurityConfig());
        this.workerCount = config.getWorkerCount();
        this.runChannel = new LogQueueChannel(LogType.RUN.name(), config.getQueueCapacity(), config.getBatchSize(), config.getBatchTimeout());
        this.traceChannel = new LogQueueChannel(LogType.TRACE.name(), config.getQueueCapacity(), config.getBatchSize(), config.getBatchTimeout());
        this.startWorkers();
    }

    public void publishRunLog(String logEvent) {
        runChannel.publish(logEvent);
    }

    public void publishTraceLog(String logEvent) {
        traceChannel.publish(logEvent);
    }

    private void startWorkers() {
        for (int i = 0; i < workerCount; i++) {
            threadPoolExecutor.execute(() -> runChannel.consume(payload -> sendBatch(RUN_TOPIC, payload)));
        }

        for (int i = 0; i < workerCount; i++) {
            threadPoolExecutor.execute(() -> traceChannel.consume(payload -> sendBatch(TRACE_TOPIC, payload)));
        }
    }

    private void sendBatch(String topic, List<String> payload) {
        if (payload.isEmpty()) {
            return;
        }
        if (circuitBreaker.isBlocked()) {
            return;
        }

        try {
            client.putMessageList(topic, payload);
        } catch (Exception e) {
            circuitBreaker.recordFailure(e.getMessage());
        }
    }

    @Override
    public void close() {
        runChannel.stop();
        traceChannel.stop();
    }
}
