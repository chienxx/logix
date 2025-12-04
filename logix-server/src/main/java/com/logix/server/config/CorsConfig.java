package com.logix.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;

/**
 * CORS 跨域配置
 *
 * @author Kanade
 * @since 2025/11/07
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许的源（开发环境）
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",     // 前端开发服务器
            "http://127.0.0.1:3000"
        ));

        // 生产环境应该配置具体的域名，例如:
        // config.setAllowedOrigins(Arrays.asList("https://logix.yourdomain.com"));

        // 允许的 HTTP 方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 允许的请求头
        config.setAllowedHeaders(Collections.singletonList("*"));

        // 允许携带凭证（Cookie）
        config.setAllowCredentials(true);

        // 预检请求的有效期（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}
