package com.logix.server.config;

import com.clickhouse.client.*;
import com.clickhouse.client.config.ClickHouseClientOption;
import com.clickhouse.client.config.ClickHouseDefaults;
import com.logix.server.config.properties.ServerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * ClickHouse HTTP Client 配置
 *
 * @author Kanade
 * @since 2025/11/20
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ClickHouseConfig {

    private final ServerProperties properties;

    /**
     * 创建 ClickHouse 节点管理器
     */
    @Bean
    public ClickHouseNodes clickHouseNodes() {
        ServerProperties.ClickHouse ch = properties.getClickhouse();
        String endpoints = ch.getUrl().replace("jdbc:clickhouse", "http");

        Map<String, String> options = new HashMap<>();
        options.put(ClickHouseDefaults.DATABASE.getKey(), ch.getDbName());
        options.put(ClickHouseDefaults.USER.getKey(), ch.getUsername());
        options.put(ClickHouseDefaults.PASSWORD.getKey(), ch.getPassword());

        // 超时设置
        options.put(ClickHouseClientOption.CONNECTION_TIMEOUT.getKey(), String.valueOf(ch.getConnectionTimeout().toMillis()));
        options.put(ClickHouseClientOption.SOCKET_TIMEOUT.getKey(), String.valueOf(ch.getSocketTimeout().toMillis()));

        // 负载均衡策略
        options.put(ClickHouseClientOption.LOAD_BALANCING_POLICY.getKey(), ClickHouseLoadBalancingPolicy.RANDOM);
        // 失败重试次数
        options.put(ClickHouseClientOption.FAILOVER.getKey(), String.valueOf(ch.getRetryCount()));

        // 初始化节点
        ClickHouseNodes nodes = ClickHouseNodes.of(endpoints, options);

        log.info("[ClickHouse] 初始化完成 | 节点: [{}] | 库: {}", endpoints, ch.getDbName());

        return nodes;
    }

    /**
     * 创建 ClickHouse HTTP 客户端
     * 客户端不绑定特定节点，使用时通过 client.read(node) 指定目标节点
     */
    @Bean
    public ClickHouseClient clickHouseClient() {
        ClickHouseClient client = ClickHouseClient.newInstance(ClickHouseProtocol.HTTP);
        log.info("[ClickHouse] HTTP Client 初始化完成");
        return client;
    }

}
