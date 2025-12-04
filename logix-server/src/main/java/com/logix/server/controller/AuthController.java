package com.logix.server.controller;

import com.logix.common.util.JsonUtils;
import com.logix.server.auth.SessionService;
import com.logix.server.config.properties.ServerProperties;
import com.logix.server.model.LoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录认证接口
 *
 * @author Kanade
 * @since 2025/11/07
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String SESSION_COOKIE_NAME = "LOGIX_SESSION";

    @Resource
    private ServerProperties properties;

    @Resource
    private SessionService sessionService;

    /**
     * 登录接口
     */
    @PostMapping("/login")
    public ResponseEntity<LoginVO> login(@RequestBody LoginVO loginVO, HttpServletResponse response) {
        log.info("用户登录请求: {}", JsonUtils.toJson(loginVO));

        // 验证用户名和密码
        ServerProperties.Auth auth = properties.getAuth();
        if (!auth.getUsername().equals(loginVO.getUsername()) ||
                !auth.getPassword().equals(loginVO.getPassword())) {
            return ResponseEntity.status(401).body(new LoginVO(loginVO.getUsername(), "用户名或密码错误"));
        }

        // 创建会话
        String sessionId = sessionService.createSession(loginVO.getUsername());

        // 设置 httpOnly Cookie
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
        cookie.setHttpOnly(true);      // 防止 XSS 攻击
        cookie.setPath("/");
        cookie.setMaxAge(auth.getSessionExpiration().intValue());
        response.addCookie(cookie);

        log.info("{}用户登录成功", loginVO.getUsername());
        return ResponseEntity.ok(new LoginVO(loginVO.getUsername(), "登录成功"));
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<LoginVO> getCurrentUser(HttpServletRequest request) {
        // 读取 Cookie 中的 sessionId
        String sessionId = getSessionIdFromCookie(request);
        if (sessionId == null) {
            return ResponseEntity.status(401).body(new LoginVO("未登录"));
        }

        // 验证 session 并获取用户名
        String username = sessionService.getUsername(sessionId);
        if (username == null) {
            return ResponseEntity.status(401).body(new LoginVO("会话已过期"));
        }

        return ResponseEntity.ok(new LoginVO(username, "已登录"));
    }

    /**
     * 登出接口
     */
    @PostMapping("/logout")
    public ResponseEntity<LoginVO> logout(HttpServletRequest request, HttpServletResponse response) {
        // 读取 Cookie 中的 sessionId
        String sessionId = getSessionIdFromCookie(request);
        if (sessionId != null) {
            sessionService.destroySession(sessionId);
        }

        // 清除 Cookie
        Cookie clearCookie = new Cookie(SESSION_COOKIE_NAME, null);
        clearCookie.setPath("/");
        clearCookie.setMaxAge(0);
        response.addCookie(clearCookie);

        return ResponseEntity.ok(new LoginVO("已退出登录"));
    }

    /**
     * 从 Cookie 中提取 sessionId
     */
    private String getSessionIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
