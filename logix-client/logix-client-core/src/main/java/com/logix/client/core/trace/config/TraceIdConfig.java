package com.logix.client.core.trace.config;

import com.logix.client.core.trace.interceptor.TraceIdInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * TraceId 拦截器配置
 *
 * @author Kanade
 * @since 2025/10/18
 */
@Configuration
public class TraceIdConfig implements WebMvcConfigurer {

    @Bean
    public TraceIdInterceptor traceIdInterceptor() {
        return new TraceIdInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        InterceptorRegistration registration = registry.addInterceptor(traceIdInterceptor());
        // 所有路径都被拦截
        registration.addPathPatterns("/**");
        // 设置最低优先级，确保在业务拦截器之后执行，以便读取业务拦截器设置的 traceId
        registration.order(Ordered.LOWEST_PRECEDENCE);
    }
}
