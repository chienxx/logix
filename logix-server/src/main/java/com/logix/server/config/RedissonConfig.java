package com.logix.server.config;

import com.logix.common.util.JsonUtils;
import com.logix.server.config.properties.ServerProperties;
import com.logix.server.config.properties.ServerProperties.Redis;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Redisson 配置
 *
 * @author Kanade
 * @since 2025/11/07
 */
@Slf4j
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(ServerProperties properties) {
        Redis redis = properties.getRedis();
        Config config = new Config();
        config.setCodec(new JsonJacksonCodec(JsonUtils.getObjectMapper()));

        switch (redis.getMode()) {
            case CLUSTER:
                configureCluster(config.useClusterServers(), redis);
                break;
            case SENTINEL:
                configureSentinel(config.useSentinelServers(), redis);
                break;
            default:
                configureSingle(config.useSingleServer(), redis);
        }

        RedissonClient redissonClient = Redisson.create(config);
        log.info("[Redisson] 客户端初始化完成 | 连接模式: {}", redis.getMode());
        return redissonClient;
    }

    private void configureSingle(SingleServerConfig config, Redis redis) {
        Redis.Single single = redis.getSingle();
        String address = String.format("redis://%s:%d", single.getHost(), single.getPort());

        config.setAddress(address)
                .setDatabase(redis.getDatabase())
                .setConnectionPoolSize(redis.getConnectionPoolSize())
                .setConnectTimeout(redis.getConnectionTimeout());

        if (StringUtils.hasText(redis.getPassword())) {
            config.setPassword(redis.getPassword());
        }
    }

    private void configureSentinel(SentinelServersConfig config, Redis redis) {
        Redis.Sentinel sentinel = redis.getSentinel();
        if (sentinel == null || !StringUtils.hasText(sentinel.getMaster())) {
            throw new IllegalArgumentException("哨兵模式必须配置 master");
        }
        if (sentinel.getNodes() == null || sentinel.getNodes().isEmpty()) {
            throw new IllegalArgumentException("哨兵模式必须配置 nodes");
        }

        config.setMasterName(sentinel.getMaster())
                .addSentinelAddress(formatNodes(sentinel.getNodes()))
                .setDatabase(redis.getDatabase())
                .setMasterConnectionPoolSize(redis.getConnectionPoolSize())
                .setConnectTimeout(redis.getConnectionTimeout());

        if (StringUtils.hasText(redis.getPassword())) {
            config.setPassword(redis.getPassword());
        }
    }

    private void configureCluster(ClusterServersConfig config, Redis redis) {
        Redis.Cluster cluster = redis.getCluster();
        if (cluster == null || cluster.getNodes() == null || cluster.getNodes().isEmpty()) {
            throw new IllegalArgumentException("集群模式必须配置 nodes");
        }

        config.addNodeAddress(formatNodes(cluster.getNodes()))
                .setMasterConnectionPoolSize(redis.getConnectionPoolSize())
                .setConnectTimeout(redis.getConnectionTimeout());

        if (StringUtils.hasText(redis.getPassword())) {
            config.setPassword(redis.getPassword());
        }
    }

    private String[] formatNodes(java.util.List<String> nodes) {
        return nodes.stream()
                .map(n -> n.startsWith("redis://") ? n : "redis://" + n)
                .toArray(String[]::new);
    }
}
