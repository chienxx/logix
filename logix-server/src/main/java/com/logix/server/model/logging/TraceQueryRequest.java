package com.logix.server.model.logging;

import lombok.Getter;
import lombok.Setter;

/**
 * 链路追踪查询请求
 *
 * @author Kanade
 * @since 2025/11/23
 */
@Getter
@Setter
public class TraceQueryRequest {

    /**
     * TraceId（必填）
     */
    private String traceId;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 开始时间（时间戳）
     */
    private Long startTime;

    /**
     * 结束时间（时间戳）
     */
    private Long endTime;
}
