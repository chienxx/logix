package com.logix.server.consumer;

import com.logix.common.constants.LogixConstants;
import com.logix.common.enums.LogType;
import com.logix.common.model.RunLogEvent;
import com.logix.common.model.TraceLogEvent;
import com.logix.common.util.ThreadPoolUtils;
import com.logix.server.config.properties.ServerProperties;
import com.logix.server.storage.writer.RunLogWriter;
import com.logix.server.storage.writer.TraceLogWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * 日志消费管道
 * 管理 Kafka 消费 → 队列缓冲 → ClickHouse 写入
 *
 * @author Kanade
 * @since 2025/11/18
 */
@Slf4j
@Component
public class LogConsumerPipeline {

    private final ServerProperties serverProperties;
    private final RunLogWriter runLogWriter;
    private final TraceLogWriter traceLogWriter;

    // 核心组件
    private LogBatchConsumer kafkaConsumer;
    private ExecutorService runLogExecutor;
    private ExecutorService traceLogExecutor;
    private LogBatchWriter<RunLogEvent> runLogBatchWriter;
    private LogBatchWriter<TraceLogEvent> traceLogBatchWriter;

    public LogConsumerPipeline(
            ServerProperties serverProperties,
            RunLogWriter runLogWriter,
            TraceLogWriter traceLogWriter) {
        this.serverProperties = serverProperties;
        this.runLogWriter = runLogWriter;
        this.traceLogWriter = traceLogWriter;
    }

    @PostConstruct
    public void start() {
        log.info("[日志消费管道] 初始化开始");

        ServerProperties.Kafka kafkaConfig = serverProperties.getKafka();
        ServerProperties.Pipeline pipelineConfig = serverProperties.getPipeline();

        // 创建两个独立的队列
        BlockingQueue<RunLogEvent> runQueue = new LinkedBlockingQueue<>(
            pipelineConfig.getRunLog().getQueueCapacity()
        );
        BlockingQueue<TraceLogEvent> traceQueue = new LinkedBlockingQueue<>(
            pipelineConfig.getTraceLog().getQueueCapacity()
        );

        log.info("[日志消费管道] 队列已创建 | RunLog容量:{} | TraceLog容量:{}",
            pipelineConfig.getRunLog().getQueueCapacity(),
            pipelineConfig.getTraceLog().getQueueCapacity());

        // 创建路由器
        LogMessageRouter router = new LogMessageRouter(runQueue, traceQueue);

        // 创建并启动Kafka消费者（订阅两个topic）
        Properties kafkaProps = buildKafkaProperties();
        this.kafkaConsumer = new LogBatchConsumer(
            kafkaProps,
            Arrays.asList(
                LogixConstants.Kafka.RUN_TOPIC,
                LogixConstants.Kafka.TRACE_TOPIC
            ),
            kafkaConfig.getGroupId(),
            router
        );
        this.kafkaConsumer.start();

        // 创建两个独立的Executor，每个专门负责一个Writer
        this.runLogExecutor = ThreadPoolUtils.createLogWriterExecutor(LogType.RUN.name());
        this.traceLogExecutor = ThreadPoolUtils.createLogWriterExecutor(LogType.TRACE.name());

        // RunLog Writer
        this.runLogBatchWriter = new LogBatchWriter<>(
            LogType.RUN.name(),
            runQueue,
            runLogWriter,
            pipelineConfig.getRunLog().getBatchSize(),
            pipelineConfig.getRunLog().getBatchTimeoutMs()
        );
        this.runLogExecutor.execute(runLogBatchWriter);

        // TraceLog Writer
        this.traceLogBatchWriter = new LogBatchWriter<>(
            LogType.TRACE.name(),
            traceQueue,
            traceLogWriter,
            pipelineConfig.getTraceLog().getBatchSize(),
            pipelineConfig.getTraceLog().getBatchTimeoutMs()
        );
        this.traceLogExecutor.execute(traceLogBatchWriter);

        log.info("[日志消费管道] 启动完成 | topics:[{}, {}] | group:{}",
            LogixConstants.Kafka.RUN_TOPIC,
            LogixConstants.Kafka.TRACE_TOPIC,
            kafkaConfig.getGroupId());
    }

    @PreDestroy
    public void shutdown() {
        log.info("[日志消费管道] 开始关闭");

        // 停止Kafka消费（停止新消息进入队列）
        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }

        // 通知Writer停止（处理完队列中剩余数据）
        if (runLogBatchWriter != null) {
            runLogBatchWriter.stop();
        }
        if (traceLogBatchWriter != null) {
            traceLogBatchWriter.stop();
        }

        runLogExecutor.shutdown();
        traceLogExecutor.shutdown();

        // 等待写入线程结束
        ThreadPoolUtils.shutdownGracefully(runLogExecutor);
        ThreadPoolUtils.shutdownGracefully(traceLogExecutor);

        log.info("[日志消费管道] 关闭完成");
    }

    /**
     * 构建Kafka配置
     */
    private Properties buildKafkaProperties() {
        ServerProperties.Kafka kafkaConfig = serverProperties.getKafka();

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConfig.getGroupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, kafkaConfig.getMaxPollRecords());
        return props;
    }
}
