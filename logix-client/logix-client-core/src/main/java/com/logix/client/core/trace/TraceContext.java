package com.logix.client.core.trace;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 链路追踪上下文，存储跨线程链路相关信息
 *
 * @author Kanade
 * @since 2025/10/17
 */
public class TraceContext {

    public static TransmittableThreadLocal<String> currentTraceID = new TransmittableThreadLocal<>();
    public static TransmittableThreadLocal<TraceMessage> currentTraceMessage = new TransmittableThreadLocal<>();
}
