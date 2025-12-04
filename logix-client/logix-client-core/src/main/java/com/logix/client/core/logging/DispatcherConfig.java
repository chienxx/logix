package com.logix.client.core.logging;

import com.logix.common.config.KafkaSecurityConfig;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 分发器配置
 *
 * @author Kanade
 * @since 2025/10/22
 */
@Getter
@Setter
@Builder
public class DispatcherConfig {

    /**
     * kafka服务器地址
     */
    private final String bootstrapServers;

    /**
     * 内存队列容量
     */
    @Builder.Default
    private final int queueCapacity = 10000;

    /**
     * 批量大小
     */
    @Builder.Default
    private final int batchSize = 100;

    /**
     * 批量超时时间（毫秒）
     */
    @Builder.Default
    private final long batchTimeout = 500;

    /**
     * 线程数量
     */
    @Builder.Default
    private final int workerCount = 1;

    /**
     * 熔断器超时时间（秒）
     */
    @Builder.Default
    private final long quietPeriod = 30;

    /**
     * Kafka 安全认证配置
     */
    private final KafkaSecurityConfig securityConfig;

}