package com.logix.server.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Session 认证拦截器
 * 负责验证请求中的 Session Cookie 并提取用户信息
 *
 * @author Kanade
 * @since 2025/11/07
 */
@Slf4j
@Component
public class SessionAuthInterceptor implements HandlerInterceptor {

    private static final String SESSION_COOKIE_NAME = "LOGIX_SESSION";

    @Resource
    private SessionService sessionService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 从 Cookie 中读取 sessionId
        String sessionId = getSessionIdFromCookie(request);
        if (sessionId == null) {
            return unauthorized(response, "未登录或会话已过期");
        }

        // 校验 Session
        if (!sessionService.validateSession(sessionId)) {
            return unauthorized(response, "会话无效或已过期");
        }

        // 续期会话
        sessionService.renewSession(sessionId);

        // 提取用户名并存入请求属性
        String username = sessionService.getUsername(sessionId);
        request.setAttribute("logix.username", username);

        return true;
    }

    /**
     * 从 Cookie 中获取 sessionId
     */
    private String getSessionIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 返回 401 未授权响应
     */
    private boolean unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, String> body = new HashMap<>();
        body.put("message", message);

        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }
}
