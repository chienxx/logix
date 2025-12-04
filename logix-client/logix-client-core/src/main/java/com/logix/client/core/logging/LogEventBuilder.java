package com.logix.client.core.logging;

import com.logix.client.core.trace.TraceContext;
import com.logix.client.core.trace.TraceMessage;
import com.logix.common.util.NetworkUtils;
import com.logix.common.model.RunLogEvent;
import com.logix.common.model.TraceLogEvent;

/**
 * 日志事件构造器
 *
 * @author Kanade
 * @since 2025/10/22
 */
public class LogEventBuilder {

    /**
     * 创建链路日志事件
     */
    public static TraceLogEvent createTraceLog(TraceMessage traceMessage, String appName, String env, long time) {
        TraceLogEvent traceLogEvent = new TraceLogEvent();
        traceLogEvent.setPosition(traceMessage.getPosition());
        traceLogEvent.setDepth(traceMessage.getDepth().get());
        traceLogEvent.setEventTime(time);
        traceLogEvent.setAppName(appName);
        traceLogEvent.setEnv(env);
        traceLogEvent.setServerIp(NetworkUtils.getLocalIP());
        traceLogEvent.setMethodName(traceMessage.getSignature());
        traceLogEvent.setTraceId(TraceContext.currentTraceID.get());

        return traceLogEvent;
    }

    /**
     * 创建运行日志事件
     */
    public static RunLogEvent createRunLog(String appName, String env, String message, long time) {
        RunLogEvent runLogEvent = new RunLogEvent();
        runLogEvent.setContent(message);
        runLogEvent.setEventTime(time);
        runLogEvent.setAppName(appName);
        runLogEvent.setEnv(env);
        runLogEvent.setServerIp(NetworkUtils.getLocalIP());
        runLogEvent.setTraceId(TraceContext.currentTraceID.get());

        return runLogEvent;
    }

    /**
     * 组装消息
     */
    public static String packageMessage(String message, Object[] args) {
        StringBuilder builder = new StringBuilder(128);
        builder.append(message);
        for (Object arg : args) {
            builder.append("\n").append(arg);
        }
        return builder.toString();
    }
}