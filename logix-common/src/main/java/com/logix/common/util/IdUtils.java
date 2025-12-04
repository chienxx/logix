package com.logix.common.util;

import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.util.Enumeration;

/**
 * ID生成工具类
 * 基于雪花算法，生成全局唯一的ID
 *
 * @author Kanade
 * @since 2025/10/18
 */
public final class IdUtils {

    private IdUtils() {
    }

    /**
     * 开始时间戳 2025-09-01
     */
    private static final long EPOCH_TIMESTAMP = 1756656000000L;

    /**
     * 机器ID位数
     */
    private static final long WORKER_ID_BITS = 5L;

    /**
     * 序列号位数
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 机器ID最大值
     */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /**
     * 序列号最大值
     */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /**
     * 机器ID左移位数
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 时间戳左移位数
     */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 工作机器ID
     */
    private static final long WORKER_ID;

    /**
     * 序列号
     */
    private static long sequence = 0L;

    /**
     * 上次生成ID的时间戳
     */
    private static long lastTimestamp = -1L;

    /**
     * 随机数生成器（用于SpanId）
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    static {
        WORKER_ID = getWorkerId();
    }

    /**
     * 生成TraceId
     * 格式：时间戳(41位) + 机器ID(5位) + 序列号(12位) = 58位，转换为16进制字符串
     */
    public static String generateTraceId() {
        return Long.toHexString(nextId()).toUpperCase();
    }

    /**
     * 生成SpanId
     * 使用安全随机数生成8位16进制字符串
     */
    public static String generateSpanId() {
        long randomLong = RANDOM.nextLong() & Long.MAX_VALUE; // 确保为正数
        return Long.toHexString(randomLong).substring(0, 8).toUpperCase();
    }

    /**
     * 生成下一个ID
     */
    private static synchronized long nextId() {
        long timestamp = getCurrentTimestamp();

        // 时钟回拨检测
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨，拒绝生成ID");
        }

        if (lastTimestamp == timestamp) {
            // 同一毫秒内，序列号自增
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 序列号溢出，等待下一毫秒
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒，序列号重置
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 组装ID
        return ((timestamp - EPOCH_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (WORKER_ID << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 等待下一毫秒
     */
    private static long waitNextMillis(long lastTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳
     */
    private static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取机器ID
     */
    private static long getWorkerId() {
        try {
            StringBuilder sb = new StringBuilder();
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();

            while (networks.hasMoreElements()) {
                NetworkInterface network = networks.nextElement();
                byte[] mac = network.getHardwareAddress();
                if (mac != null) {
                    for (byte b : mac) {
                        sb.append(String.format("%02X", b));
                    }
                    break;
                }
            }

            if (sb.length() == 0) {
                // 没有找到MAC地址，使用随机数
                return new SecureRandom().nextInt((int) MAX_WORKER_ID);
            }

            // 使用MAC地址的hash值作为机器ID
            return Math.abs(sb.toString().hashCode()) % (MAX_WORKER_ID + 1);
        } catch (Exception e) {
            // 异常情况下使用随机数
            return new SecureRandom().nextInt((int) MAX_WORKER_ID);
        }
    }

    /**
     * 解析TraceId中的时间戳（用于调试）
     */
    public static long parseTimestamp(String traceId) {
        try {
            long id = Long.parseLong(traceId, 16);
            return ((id >> TIMESTAMP_SHIFT) & ~(-1L << 41)) + EPOCH_TIMESTAMP;
        } catch (Exception e) {
            return -1L;
        }
    }
}