package com.logix.server.model.logging;

import lombok.Getter;
import lombok.Setter;

/**
 * 日志实时拉取请求
 *
 * @author Kanade
 * @since 2025/11/24
 */
@Getter
@Setter
public class LogRealtimeRequest implements LogFilterCriteria {

    /**
     * 过滤条件
     */
    private String appName;
    private String env;
    private String level;
    private String traceId;
    private String keyword;
    private String serverIp;
    private Long startTime;
    private Long endTime;

    /**
     * 游标：上一条日志的时间戳（毫秒）
     */
    private Long cursorDt;

    /**
     * 游标：同一毫秒内的序号
     */
    private Long cursorSeq;

    /**
     * 最大返回数量
     */
    private Integer limit = 200;
}
