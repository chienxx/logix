package com.logix.client.core.kafka;

import com.logix.common.config.KafkaSecurityConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.kafka.clients.producer.KafkaProducer;

import java.time.Duration;

import static com.logix.common.constants.LogixConstants.Kafka.*;

/**
 * Kafka生产者连接池
 *
 * @author Kanade
 * @since 2025/10/14
 */
@Slf4j
class KafkaProducerPool implements AutoCloseable {

    private final GenericObjectPool<KafkaProducer<String, String>> internalPool;

    public KafkaProducerPool(String bootstrapServers, KafkaSecurityConfig securityConfig) {
        // 池配置
        GenericObjectPoolConfig<KafkaProducer<String, String>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMinIdle(POOL_MIN_IDLE);
        poolConfig.setMaxIdle(POOL_MAX_IDLE);
        poolConfig.setMaxTotal(POOL_MAX_TOTAL);
        poolConfig.setMaxWait(Duration.ofSeconds(POOL_MAX_WAIT));

        // 创建内部连接池
        this.internalPool = new GenericObjectPool<>(
                new KafkaProducerFactory(bootstrapServers, securityConfig), poolConfig);
    }

    public KafkaProducer<String, String> borrowObject() throws Exception {
        return internalPool.borrowObject();
    }

    public void returnObject(KafkaProducer<String, String> producer) {
        internalPool.returnObject(producer);
    }

    public void returnBrokenObject(KafkaProducer<String, String> producer) {
        try {
            internalPool.invalidateObject(producer);
        } catch (Exception ignore) {
        }
    }

    @Override
    public void close() {
        internalPool.close();
    }
}