package com.logix.common.constants;


/**
 * Logix系统常量定义
 *
 * @author Kanade
 * @since 2025/09/23
 */
public final class LogixConstants {

    private LogixConstants() {
    }

    /**
     * Kafka相关常量
     */
    public static final class Kafka {
        // Topic配置
        public static final String RUN_TOPIC = "logix-run-logs";
        public static final String TRACE_TOPIC = "logix-trace-logs";

        // 生产者配置
        public static final int BATCH_SIZE = 65536;  // 64KB - 批次大小
        public static final int LINGER_MS = 10;  // 10ms - 等待时间，平衡延迟和吞吐量
        public static final long BUFFER_MEMORY = 33554432L;  // 32MB - 缓冲区内存
        public static final String COMPRESSION_TYPE = "lz4";  // 压缩算法
        public static final String ACKS = "0";  // 无确认，最高性能
        public static final int MAX_BLOCK_MS = 5000;  // 避免应用长时间阻塞
        public static final int REQUEST_TIMEOUT_MS = 15000;  // 15秒 - 请求超时
        public static final int RETRIES = 0;  // 不重试，与ACK=0保持一致

        // 连接池配置
        public static final int POOL_MIN_IDLE = 0;
        public static final int POOL_MAX_IDLE = 8;
        public static final int POOL_MAX_TOTAL = 30;
        public static final long POOL_MAX_WAIT = 1;  // 1秒

        private Kafka() {
        }
    }

    /**
     * 链路追踪相关常量
     */
    public static final class Trace {
        public static final String START = "<";  // 链路开始标志
        public static final String END = ">";  // 链路结束标志
        public static final String PREFIX = "TRACE:";  // 链路日志前缀
        public static final String ID_KEY = "traceId";  // 链路ID在MDC中的key

        private Trace() {
        }
    }

    /**
     * 通用常量
     */
    public static final class Common {
        public static final String PLACEHOLDER = "{}";  // 日志占位符

        private Common() {
        }
    }
}
