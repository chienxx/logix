package com.logix.server.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * 服务端配置
 *
 * @author Kanade
 * @since 2025/11/07
 */
@Getter
@ConfigurationProperties(prefix = "logix")
public class ServerProperties {

    private final Auth auth = new Auth();
    private final Kafka kafka = new Kafka();
    private final ClickHouse clickhouse = new ClickHouse();
    private final Redis redis = new Redis();
    private final Pipeline pipeline = new Pipeline();

    @Getter
    @Setter
    public static class Auth {
        private String username;
        private String password;
        private Long sessionExpiration;
    }

    @Getter
    @Setter
    public static class Kafka {
        private String bootstrapServers;
        private String groupId;
        private String username;
        private String password;
        private Integer maxPollRecords = 5000;
    }

    @Getter
    @Setter
    public static class ClickHouse {
        private String url;
        private String dbName;
        private String username;
        private String password;
        private Duration connectionTimeout = Duration.ofSeconds(5);
        private Duration socketTimeout = Duration.ofSeconds(30);
        private Integer retryCount = 2;
    }

    @Getter
    @Setter
    public static class Redis {
        private Mode mode = Mode.SINGLE;
        private String password;
        private Integer database = 0;
        private Integer connectionTimeout = 10000;
        private Integer connectionPoolSize = 64;

        private Single single = new Single();
        private Sentinel sentinel;
        private Cluster cluster;

        public enum Mode { SINGLE, SENTINEL, CLUSTER }

        @Getter
        @Setter
        public static class Single {
            private String host = "localhost";
            private Integer port = 6379;
        }

        @Getter
        @Setter
        public static class Sentinel {
            private String master;
            private List<String> nodes;
        }

        @Getter
        @Setter
        public static class Cluster {
            private List<String> nodes;
        }
    }

    @Getter
    @Setter
    public static class Pipeline {
        private LogPipeline runLog = new LogPipeline(10000, 2000, 5000L);
        private LogPipeline traceLog = new LogPipeline(5000, 1000, 10000L);

        @Getter
        @Setter
        public static class LogPipeline {
            private Integer queueCapacity;
            private Integer batchSize;
            private Long batchTimeoutMs;//超过此时间即使未达到batchSize，也写入。单位毫秒

            public LogPipeline(Integer queueCapacity, Integer batchSize, Long batchTimeoutMs) {
                this.queueCapacity = queueCapacity;
                this.batchSize = batchSize;
                this.batchTimeoutMs = batchTimeoutMs;
            }
        }
    }
}
