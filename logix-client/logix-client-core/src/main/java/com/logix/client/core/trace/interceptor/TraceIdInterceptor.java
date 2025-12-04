package com.logix.client.core.trace.interceptor;

import com.logix.client.core.trace.TraceContext;
import com.logix.common.constants.LogixConstants;
import com.logix.common.util.IdUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * TraceId拦截器
 *
 * @author Kanade
 * @since 2025/10/18
 */
public class TraceIdInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = request.getHeader(LogixConstants.Trace.ID_KEY);
        if (StringUtils.hasText(traceId)) {
            TraceContext.currentTraceID.set(traceId);
        } else {
            TraceContext.currentTraceID.set(IdUtils.generateTraceId());
        }
        return true;
    }
}
