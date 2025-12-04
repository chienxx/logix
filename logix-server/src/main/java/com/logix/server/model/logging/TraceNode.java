package com.logix.server.model.logging;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 链路追踪节点
 *
 * @author Kanade
 * @since 2025/11/24
 */
@Getter
@Setter
public class TraceNode {

    /**
     * TraceId
     */
    private String traceId;

    /**
     * 方法签名
     */
    private String signature;

    /**
     * 调用深度
     */
    private Integer depth;

    /**
     * 开始时间
     */
    private Long startTime;

    /**
     * 结束时间
     */
    private Long endTime;

    /**
     * 耗时（毫秒）
     */
    private Long duration;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 服务器IP
     */
    private String serverIp;

    /**
     * 子节点
     */
    private List<TraceNode> children = new ArrayList<>();
}
