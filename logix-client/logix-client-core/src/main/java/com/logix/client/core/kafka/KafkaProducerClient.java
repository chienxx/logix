package com.logix.client.core.kafka;

import com.logix.client.core.kafka.exception.KafkaConnectException;
import com.logix.common.config.KafkaSecurityConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.List;

/**
 * Kafka生产者客户端
 *
 * @author Kanade
 * @since 2025/10/14
 */
@Slf4j
public class KafkaProducerClient {

    private static volatile KafkaProducerClient instance;
    private final KafkaProducerPool producerPool;

    private KafkaProducerClient(String bootstrapServers, KafkaSecurityConfig securityConfig) {
        this.producerPool = new KafkaProducerPool(bootstrapServers, securityConfig);
    }

    public static KafkaProducerClient getInstance(String bootstrapServers, KafkaSecurityConfig securityConfig) {
        if (instance == null) {
            synchronized (KafkaProducerClient.class) {
                if (instance == null) {
                    instance = new KafkaProducerClient(bootstrapServers, securityConfig);
                }
            }
        }
        return instance;
    }

    public KafkaProducer<String, String> getProducer() {
        try {
            return producerPool.borrowObject();
        } catch (Exception e) {
            return null;
        }
    }

    public void returnProducer(KafkaProducer<String, String> producer) {
        try {
            producerPool.returnObject(producer);
        } catch (Exception e) {
            producerPool.returnBrokenObject(producer);
        }
    }

    public void putMessageList(String topic, List<String> messageList) throws KafkaConnectException {
        KafkaProducer<String, String> kafkaProducer = null;
        try {
            kafkaProducer = this.getProducer();
            for (String message : messageList) {
                kafkaProducer.send(new ProducerRecord<>(topic, message));
            }
        } catch (Exception e) {
            throw new KafkaConnectException("Kafka写入失败", e);
        } finally {
            if (kafkaProducer != null) {
                this.returnProducer(kafkaProducer);
            }
        }
    }

}