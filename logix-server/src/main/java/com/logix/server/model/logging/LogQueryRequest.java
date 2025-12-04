package com.logix.server.model.logging;

import lombok.Getter;
import lombok.Setter;

/**
 * 日志查询请求
 *
 * @author Kanade
 * @since 2025/11/24
 */
@Getter
@Setter
public class LogQueryRequest implements LogFilterCriteria {

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 运行环境
     */
    private String env;

    /**
     * 日志级别
     */
    private String level;

    /**
     * TraceId
     */
    private String traceId;

    /**
     * 日志内容关键字
     */
    private String keyword;

    /**
     * 服务器IP
     */
    private String serverIp;

    /**
     * 开始时间（时间戳）
     */
    private Long startTime;

    /**
     * 结束时间（时间戳）
     */
    private Long endTime;

    /**
     * 页码（从1开始）
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 50;

    /**
     * 排序字段（默认按时间倒序，同时间按 seq 倒序）
     */
    private String orderBy = "event_time DESC";
}
