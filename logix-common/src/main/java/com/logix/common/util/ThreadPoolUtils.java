package com.logix.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * 线程池工具类
 *
 * @author Kanade
 * @since 2025/10/22
 */
@Slf4j
public class ThreadPoolUtils {

    private ThreadPoolUtils() {
    }

    /**
     * 创建默认线程池
     * 核心线程数10，最大线程数10，队列容量1
     */
    public static ThreadPoolExecutor createDefaultExecutor() {
        return new ThreadPoolExecutor(
                10, 10, 10, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    /**
     * 创建 Kafka 消费者线程池
     * 单线程执行器，用于 Kafka 消息轮询
     */
    public static ExecutorService createKafkaConsumerExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "logix-kafka-consumer");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 创建日志写入线程池
     * 单线程执行器，用于日志写入
     */
    public static ExecutorService createLogWriterExecutor(String threadName) {
        return Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, threadName + "-writer");
            thread.setDaemon(false); // 非守护线程，确保日志写入完成
            return thread;
        });
    }

    /**
     * 关闭流程：
     * 1. 调用shutdown()停止接收新任务
     * 2. 等待现有任务完成（最多等待指定超时时间）
     * 3. 超时后调用shutdownNow()强制中断
     */
    public static void shutdownGracefully(ExecutorService executor) {
        if (executor == null) {
            return;
        }

        executor.shutdown();

        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}