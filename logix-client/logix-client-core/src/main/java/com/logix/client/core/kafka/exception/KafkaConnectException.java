package com.logix.client.core.kafka.exception;

/**
 * kafka连接异常
 *
 * @author Kanade
 * @since 2025/10/19
 */
public class KafkaConnectException extends Exception {

    public KafkaConnectException(String message, Throwable cause) {
        super(message, cause);
    }
}