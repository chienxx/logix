package com.logix.server;

import com.logix.server.config.properties.ServerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Logix服务端启动类
 *
 * @author Kanade
 * @since 2025/11/07
 */
@SpringBootApplication
@EnableConfigurationProperties(ServerProperties.class)
public class LogixServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogixServerApplication.class, args);
    }
}
