package com.logix.common.enums;

/**
 * 日志类型枚举
 *
 * @author Kanade
 * @since 2025/09/23
 */
public enum LogType {

    /**
     * 运行日志 - 常规业务日志
     */
    RUN,

    /**
     * 链路追踪日志 - 基于注解的方法调用追踪
     */
    TRACE
}