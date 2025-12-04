package com.logix.server.consumer;

import com.fasterxml.jackson.databind.ObjectReader;
import com.logix.common.constants.LogixConstants;
import com.logix.common.model.RunLogEvent;
import com.logix.common.model.TraceLogEvent;
import com.logix.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.util.concurrent.BlockingQueue;

/**
 * 根据topic将消息路由到对应队列
 *
 * @author Kanade
 * @since 2025/11/20
 */
@Slf4j
public class LogMessageRouter {

    private final ObjectReader runReader = JsonUtils.getObjectMapper().readerFor(RunLogEvent.class);
    private final ObjectReader traceReader = JsonUtils.getObjectMapper().readerFor(TraceLogEvent.class);

    private final BlockingQueue<RunLogEvent> runQueue;
    private final BlockingQueue<TraceLogEvent> traceQueue;

    public LogMessageRouter(
            BlockingQueue<RunLogEvent> runQueue,
            BlockingQueue<TraceLogEvent> traceQueue) {
        this.runQueue = runQueue;
        this.traceQueue = traceQueue;
    }

    public void route(ConsumerRecords<String, String> records) {

        for (ConsumerRecord<String, String> record : records) {
            String topic = record.topic();
            String payload = record.value();

            if (LogixConstants.Kafka.RUN_TOPIC.equals(topic)) {
                routeToRunQueue(payload);
            } else {
                routeToTraceQueue(payload);
            }
        }

    }

    private void routeToRunQueue(String payload) {
        RunLogEvent event = parse(payload, runReader);
        if (event == null) {
            log.warn("[RunLog] 解析失败: {}", payload);
            return;
        }
        boolean offered = runQueue.offer(event);

        if (!offered) {
            log.warn("[RunLog] 队列已满，丢弃消息");
        }
    }

    private void routeToTraceQueue(String payload) {
        TraceLogEvent event = parse(payload, traceReader);
        if (event == null) {
            log.warn("[TraceLog] 解析失败: {}", payload);
            return;
        }
        boolean offered = traceQueue.offer(event);

        if (!offered) {
            log.warn("[TraceLog] 队列已满，丢弃消息");
        }
    }

    private <T> T parse(String payload, ObjectReader reader) {
        try {
            return reader.readValue(payload);
        } catch (Exception e) {
            return null;
        }
    }
}
