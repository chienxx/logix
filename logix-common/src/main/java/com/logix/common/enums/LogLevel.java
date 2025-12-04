package com.logix.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 日志级别枚举
 *
 * @author Kanade
 * @since 2025/09/23
 */
@Getter
@AllArgsConstructor
public enum LogLevel {

    TRACE(0, "TRACE"),
    DEBUG(1, "DEBUG"),
    INFO(2, "INFO"),
    WARN(3, "WARN"),
    ERROR(4, "ERROR");

    private final int code;
    private final String name;
}