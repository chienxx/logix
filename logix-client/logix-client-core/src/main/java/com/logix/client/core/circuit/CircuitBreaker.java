package com.logix.client.core.circuit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * 断路器，防止重复失败
 *
 * @author Kanade
 * @since 2025/10/25
 */
@Slf4j
public class CircuitBreaker {

    private static final String FAILURE_KEY = "send_failed";

    private final Cache<String, Boolean> statusCache;

    public CircuitBreaker(Duration quietPeriod) {
        this.statusCache = Caffeine.newBuilder()
                .expireAfterWrite(quietPeriod)
                .maximumSize(1)
                .build();
    }

    /**
     * 检查是否处于熔断状态
     */
    public boolean isBlocked() {
        return statusCache.getIfPresent(FAILURE_KEY) != null;
    }

    /**
     * 记录失败并触发断路器
     */
    public void recordFailure(String error) {
        statusCache.put(FAILURE_KEY, true);
        log.debug("[断路器] 触发熔断，进入静默期: {}", error);
    }

}