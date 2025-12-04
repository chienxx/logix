package com.logix.server.model.logging;

import lombok.Getter;
import lombok.Setter;

/**
 * 日志记录
 *
 * @author Kanade
 * @since 2025/11/24
 */
@Getter
@Setter
public class LogRecord {

    /**
     * 日志时间（毫秒时间戳）
     */
    private Long dt;

    /**
     * 序列号（用于同一毫秒内的排序）
     */
    private Long seq;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 运行环境
     */
    private String env;

    /**
     * 服务器IP
     */
    private String serverIp;

    /**
     * 日志级别
     */
    private String level;

    /**
     * 日志内容
     */
    private String content;

    /**
     * TraceId
     */
    private String traceId;

    /**
     * 类名
     */
    private String className;

    /**
     * 方法名
     */
    private String method;

    /**
     * 线程名
     */
    private String threadName;
}
