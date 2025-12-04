package com.logix.client.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.logix.client.core.logging.DispatcherConfig;
import com.logix.client.core.logging.LogEventDispatcher;
import com.logix.common.config.KafkaSecurityConfig;
import com.logix.common.enums.LogType;
import com.logix.common.model.BaseLogEvent;
import com.logix.common.util.JsonUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 扩展logback日志输出，发送到Kafka异步处理
 *
 * @author Kanade
 * @since 2025/09/22
 */
@Slf4j
public class LogixAppender extends AppenderBase<ILoggingEvent> {

    @Setter private String appName;
    @Setter private String env;
    @Setter private String bootstrapServers;
    @Setter private String username;
    @Setter private String password;

    private LogEventDispatcher dispatcher;

    @Override
    public void start() {
        super.start();
        if (appName == null || appName.trim().isEmpty()) {
            log.warn("[LogixAppender] appName is empty");
            return;
        }
        if (bootstrapServers == null || bootstrapServers.trim().isEmpty()) {
            log.warn("[LogixAppender] bootstrapServers is empty");
            return;
        }
        DispatcherConfig config = DispatcherConfig.builder()
                .bootstrapServers(bootstrapServers.trim())
                .securityConfig(KafkaSecurityConfig.builder()
                        .username(username)
                        .password(password)
                        .build())
                .build();
        this.dispatcher = new LogEventDispatcher(config);
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) {
            return;
        }
        BaseLogEvent logEvent = LogEventConverter.convertLog(appName, env, event);
        if (logEvent.getLogType() == LogType.RUN) {
            String message = LogEventConverter.extendMessage(logEvent, event);
            dispatcher.publishRunLog(message);
        } else {
            dispatcher.publishTraceLog(JsonUtils.toJson(logEvent));
        }
    }
}
