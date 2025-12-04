package com.logix.server.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * MVC 配置，用于注册拦截器
 *
 * @author Kanade
 * @since 2025/11/07
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private SessionAuthInterceptor sessionAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionAuthInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/auth/login",
                "/api/auth/logout",
                "/api/auth/me",
                "/actuator/**"
            );
    }
}
