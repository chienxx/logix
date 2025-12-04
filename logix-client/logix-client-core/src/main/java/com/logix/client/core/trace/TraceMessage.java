package com.logix.client.core.trace;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 链路追踪消息
 *
 * @author Kanade
 * @since 2025/10/16
 */
@Getter
@Setter
public class TraceMessage {

    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * 切面方法的完整签名
     */
    private String signature;

    /**
     * 执行的位置
     */
    private String position;

    /**
     * 调用深度
     */
    private AtomicInteger depth = new AtomicInteger(0);
}