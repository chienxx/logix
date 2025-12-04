package com.logix.common.config;

import lombok.Builder;
import lombok.Getter;

import java.util.Properties;

/**
 * Kafka 安全认证配置
 *
 * @author Kanade
 * @since 2025/10/09
 */
@Getter
@Builder
public class KafkaSecurityConfig {

    private final String username;
    private final String password;

    /**
     * 是否启用认证
     */
    public boolean isEnabled() {
        return hasText(username) && hasText(password);
    }

    /**
     * 应用安全配置到 Kafka Properties
     */
    public void applyTo(Properties props) {
        if (!isEnabled()) {
            return;
        }

        props.put("security.protocol", "SASL_PLAINTEXT");
        props.put("sasl.mechanism", "PLAIN");
        props.put("sasl.jaas.config", buildJaasConfig());
    }

    private String buildJaasConfig() {
        return String.format(
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                username, password
        );
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}