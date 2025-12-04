package com.logix.server.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 * 提供常用的 Redis 操作方法
 *
 * @author Kanade
 * @since 2025/11/08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedissonClient redissonClient;

    /**
     * 设置键值
     */
    public <T> void set(String key, T value) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value);
    }

    /**
     * 设置键值并指定过期时间
     */
    public <T> void set(String key, T value, Duration duration) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value, duration);
    }

    /**
     * 获取值
     */
    public <T> T get(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    /**
     * 删除键（支持所有类型）
     */
    public boolean delete(String key) {
        return redissonClient.getKeys().delete(key) > 0;
    }

    /**
     * 批量删除键
     */
    public long delete(String... keys) {
        return redissonClient.getKeys().delete(keys);
    }

    /**
     * 判断键是否存在（支持所有类型）
     */
    public boolean exists(String key) {
        return redissonClient.getKeys().countExists(key) > 0;
    }

    /**
     * 设置过期时间（支持所有类型）
     */
    public boolean expire(String key, Duration duration) {
        return redissonClient.getKeys().expire(key, duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 获取剩余过期时间（毫秒）
     */
    public long ttl(String key) {
        return redissonClient.getKeys().remainTimeToLive(key);
    }

    /**
     * Hash 设置字段值
     */
    public <K, V> void hSet(String key, K field, V value) {
        RMap<K, V> map = redissonClient.getMap(key);
        map.put(field, value);
    }

    /**
     * Hash 获取字段值
     */
    public <K, V> V hGet(String key, K field) {
        RMap<K, V> map = redissonClient.getMap(key);
        return map.get(field);
    }

    /**
     * Hash 删除字段
     */
    public <K> void hDelete(String key, K field) {
        RMap<K, ?> map = redissonClient.getMap(key);
        map.remove(field);
    }

    /**
     * Hash 获取所有字段和值
     */
    public <K, V> Map<K, V> hGetAll(String key) {
        RMap<K, V> map = redissonClient.getMap(key);
        return map.readAllMap();
    }

    /**
     * Hash 批量设置
     */
    public <K, V> void hSetAll(String key, Map<K, V> values) {
        RMap<K, V> map = redissonClient.getMap(key);
        map.putAll(values);
    }

    /**
     * Hash 判断字段是否存在
     */
    public <K> boolean hExists(String key, K field) {
        RMap<K, ?> map = redissonClient.getMap(key);
        return map.containsKey(field);
    }

    /**
     * 尝试获取锁（立即返回）
     *
     * @param lockKey 锁键
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey) {
        return redissonClient.getLock(lockKey).tryLock();
    }

    /**
     * 尝试获取锁（等待指定时间）
     *
     * @param lockKey   锁键
     * @param waitTime  等待时间
     * @param leaseTime 锁持有时间
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, Duration waitTime, Duration leaseTime) {
        try {
            return redissonClient.getLock(lockKey)
                    .tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Redis] 获取锁被中断 | key:{}", lockKey, e);
            return false;
        }
    }

    /**
     * 释放锁
     */
    public void unlock(String lockKey) {
        try {
            redissonClient.getLock(lockKey).unlock();
        } catch (IllegalMonitorStateException e) {
            log.warn("[Redis] 释放锁失败,锁不属于当前线程 | key:{}", lockKey);
        }
    }

    /**
     * 递增
     */
    public long increment(String key) {
        return redissonClient.getAtomicLong(key).incrementAndGet();
    }

    /**
     * 递增指定值
     */
    public long increment(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(delta);
    }

    /**
     * 递减
     */
    public long decrement(String key) {
        return redissonClient.getAtomicLong(key).decrementAndGet();
    }

    /**
     * 递减指定值
     */
    public long decrement(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(-delta);
    }

    /**
     * 发布消息到频道
     *
     * @param channel 频道名称
     * @param message 消息内容
     * @return 接收到消息的订阅者数量
     */
    public <T> long publish(String channel, T message) {
        return redissonClient.getTopic(channel).publish(message);
    }

    /**
     * 订阅频道并处理消息
     *
     * @param channel  频道名称
     * @param type     消息类型
     * @param listener 消息监听器
     * @return 监听器ID（用于取消订阅）
     */
    public <T> int subscribe(String channel, Class<T> type, MessageListener<T> listener) {
        return redissonClient.getTopic(channel).addListener(type, (ch, msg) -> {
            try {
                listener.onMessage(channel, type.cast(msg));
            } catch (Exception e) {
                log.error("[Redis] 处理订阅消息失败 | channel:{}", channel, e);
            }
        });
    }


    /**
     * 取消订阅
     *
     * @param channel    频道名称
     * @param listenerId 监听器ID
     */
    public void unsubscribe(String channel, int listenerId) {
        redissonClient.getTopic(channel).removeListener(listenerId);
    }

    /**
     * 消息监听器接口
     */
    @FunctionalInterface
    public interface MessageListener<T> {
        void onMessage(String channel, T message);
    }
}
