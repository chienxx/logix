package com.logix.client.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.logix.common.constants.LogixConstants;
import com.logix.client.core.trace.TraceContext;
import com.logix.client.core.trace.TraceMessage;
import com.logix.client.core.logging.LogEventBuilder;
import com.logix.common.enums.LogLevel;
import com.logix.common.model.BaseLogEvent;
import com.logix.common.model.RunLogEvent;
import com.logix.common.util.JsonUtils;
import org.slf4j.helpers.MessageFormatter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Logback事件到Logix事件的转换器
 *
 * @author Kanade
 * @since 2025/09/22
 */
public class LogEventConverter {

    /**
     * 序列生成器：保证同一毫秒内的日志按顺序显示
     */
    private static final AtomicLong SEQ_BUILDER = new AtomicLong();

    /**
     * 从MDC中提取TraceID并设置到上下文
     */
    private static void extractTraceIdFromMDC(ILoggingEvent logEvent) {
        if (logEvent.getMDCPropertyMap().isEmpty()) {
            return;
        }

        String traceId = logEvent.getMDCPropertyMap().get(LogixConstants.Trace.ID_KEY);
        if (traceId != null) {
            TraceContext.currentTraceID.set(traceId);
        }
    }


    /**
     * 转换Logback事件为Logix事件
     */
    public static BaseLogEvent convertLog(final String appName, final String env, final ILoggingEvent iLoggingEvent) {
        // 从MDC提取TraceID
        extractTraceIdFromMDC(iLoggingEvent);

        // 获取消息内容
        String formattedMessage = getMessage(iLoggingEvent);

        // 判断是否为Trace日志
        if (formattedMessage.startsWith(LogixConstants.Trace.PREFIX)) {
            TraceMessage traceMessage = TraceContext.currentTraceMessage.get();
            return LogEventBuilder.createTraceLog(traceMessage, appName, env, iLoggingEvent.getTimeStamp());
        }

        // 创建运行时日志
        RunLogEvent runLogEvent = LogEventBuilder.createRunLog(appName, env, formattedMessage, iLoggingEvent.getTimeStamp());

        // 补充Logback特有的元数据
        runLogEvent.setClassName(iLoggingEvent.getLoggerName());
        runLogEvent.setThreadName(iLoggingEvent.getThreadName());
        runLogEvent.setSeq(SEQ_BUILDER.getAndIncrement());
        runLogEvent.setMethodName(extractMethodName(iLoggingEvent));
        runLogEvent.setLogLevel(convertLogLevel(iLoggingEvent.getLevel()));

        return runLogEvent;
    }

    /**
     * 扩展字段
     */
    public static String extendMessage(BaseLogEvent logEvent, final ILoggingEvent iLoggingEvent) {
        Map<String, String> mdc = iLoggingEvent.getMDCPropertyMap();
        Map<String, Object> map = JsonUtils.toMap(logEvent);
        if (mdc != null) {
            map.putAll(mdc);
        }
        return JsonUtils.toJson(map);
    }

    /**
     * 获取格式化后的日志消息
     */
    private static String getMessage(ILoggingEvent logEvent) {
        Level level = logEvent.getLevel();

        // ERROR和WARN级别需要包含堆栈信息
        if (level.equals(Level.ERROR) || level.equals(Level.WARN)) {
            if (logEvent.getThrowableProxy() != null) {
                ThrowableProxy throwableProxy = (ThrowableProxy) logEvent.getThrowableProxy();
                String stackTrace = errorStackTrace(throwableProxy.getThrowable());
                return logEvent.getFormattedMessage() + "\n" + stackTrace;
            } else {
                Object[] args = logEvent.getArgumentArray();
                if (args != null) {
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] instanceof Throwable) {
                            args[i] = errorStackTrace(args[i]);
                        }
                    }
                    return packageMessage(logEvent.getMessage(), args);
                }
            }
        }

        return logEvent.getFormattedMessage();
    }

    private static String packageMessage(String message, Object[] args) {
        if (message != null && message.contains(LogixConstants.Common.PLACEHOLDER)) {
            return MessageFormatter.arrayFormat(message, args).getMessage();
        }
        return LogEventBuilder.packageMessage(message, args);
    }

    /**
     * 获取异常堆栈信息
     */
    private static String errorStackTrace(Object obj) {
        if (!(obj instanceof Throwable)) {
            return String.valueOf(obj);
        }

        Throwable throwable = (Throwable) obj;
        try (StringWriter sw = new StringWriter();
             PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            return sw.toString();
        } catch (IOException e) {
            return throwable.toString();
        }
    }

    /**
     * 提取方法名
     */
    private static String extractMethodName(ILoggingEvent logEvent) {
        StackTraceElement[] callerData = logEvent.getCallerData();
        if (callerData != null && callerData.length > 0) {
            return callerData[0].getMethodName();
        }
        return "unknown";
    }

    /**
     * 转换日志级别
     */
    private static LogLevel convertLogLevel(Level level) {
        switch (level.toInt()) {
            case Level.TRACE_INT:
                return LogLevel.TRACE;
            case Level.DEBUG_INT:
                return LogLevel.DEBUG;
            case Level.WARN_INT:
                return LogLevel.WARN;
            case Level.ERROR_INT:
                return LogLevel.ERROR;
            default:
                return LogLevel.INFO;
        }
    }
}
