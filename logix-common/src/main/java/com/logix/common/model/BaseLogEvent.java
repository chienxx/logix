package com.logix.common.model;

import com.logix.common.enums.LogType;
import lombok.Getter;
import lombok.Setter;

/**
 * 日志事件基类
 *
 * @author Kanade
 * @since 2025/09/26
 */
@Getter
@Setter
public abstract class BaseLogEvent {

    /**
     * 日志产生时间 (时间戳)
     */
    private Long eventTime;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 运行环境 (dev/test/prod)
     */
    private String env;

    /**
     * 服务器IP
     */
    private String serverIp;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * 获取日志类型
     * 由子类实现，用于序列化和路由
     */
    public abstract LogType getLogType();
}