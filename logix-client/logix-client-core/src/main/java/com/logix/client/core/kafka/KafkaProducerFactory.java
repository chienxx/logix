package com.logix.client.core.kafka;

import com.logix.common.config.KafkaSecurityConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

import static com.logix.common.constants.LogixConstants.Kafka.*;

/**
 * Kafka生产者工厂
 *
 * @author Kanade
 * @since 2025/10/14
 */
@Slf4j
public class KafkaProducerFactory extends BasePooledObjectFactory<KafkaProducer<String, String>> {

    private final Properties props;

    public KafkaProducerFactory(String bootstrapServers, KafkaSecurityConfig securityConfig) {
        this.props = createProperties(bootstrapServers, securityConfig);
    }

    @Override
    public KafkaProducer<String, String> create() {
        return new KafkaProducer<>(props);
    }

    @Override
    public PooledObject<KafkaProducer<String, String>> wrap(KafkaProducer<String, String> producer) {
        return new DefaultPooledObject<>(producer);
    }

    @Override
    public void destroyObject(PooledObject<KafkaProducer<String, String>> pool) {
        KafkaProducer<String, String> producer = pool.getObject();
        if (producer != null) {
            producer.close();
        }
    }

    private Properties createProperties(String bootstrapServers, KafkaSecurityConfig securityConfig) {
        Properties props = new Properties();

        // 基础配置
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // 性能配置
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, BATCH_SIZE);
        props.put(ProducerConfig.LINGER_MS_CONFIG, LINGER_MS);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, BUFFER_MEMORY);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, COMPRESSION_TYPE);

        // 可靠性配置
        props.put(ProducerConfig.ACKS_CONFIG, ACKS);
        props.put(ProducerConfig.RETRIES_CONFIG, RETRIES);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, MAX_BLOCK_MS);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, REQUEST_TIMEOUT_MS);

        // 安全认证配置
        securityConfig.applyTo(props);

        return props;
    }
}