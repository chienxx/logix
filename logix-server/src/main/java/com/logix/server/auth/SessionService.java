package com.logix.server.auth;

import com.logix.common.util.JsonUtils;
import com.logix.server.config.properties.ServerProperties;
import com.logix.server.redis.RedisService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.Duration;
import java.util.UUID;

/**
 * Session 管理服务
 * 基于 Redis 存储用户会话信息
 *
 * @author Kanade
 * @since 2025/11/07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionService {

    private static final String SESSION_KEY_PREFIX = "logix:session:";

    private final ServerProperties properties;

    private final RedisService redisService;

    /**
     * 创建用户会话
     *
     * @param username 用户名
     * @return sessionId
     */
    public String createSession(String username) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");

        SessionData data = new SessionData();
        data.setUsername(username);
        data.setLoginTime(System.currentTimeMillis());

        String key = SESSION_KEY_PREFIX + sessionId;
        String json = JsonUtils.toJson(data);
        redisService.set(key, json, Duration.ofSeconds(properties.getAuth().getSessionExpiration()));
        log.info("创建会话成功，username：{}，sessionId：{}", username, sessionId);
        return sessionId;
    }

    /**
     * 验证会话是否有效
     *
     * @param sessionId 会话ID
     * @return 是否有效
     */
    public boolean validateSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return false;
        }

        String key = SESSION_KEY_PREFIX + sessionId;
        return redisService.exists(key);
    }

    /**
     * 获取会话中的用户名
     *
     * @param sessionId 会话ID
     * @return 用户名，如果会话不存在返回 null
     */
    public String getUsername(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }

        String key = SESSION_KEY_PREFIX + sessionId;
        String json = redisService.get(key);

        if (json == null) {
            return null;
        }

        SessionData data = JsonUtils.fromJson(json, SessionData.class);
        if (data == null) {
            return null;
        }
        return data.getUsername();
    }

    /**
     * 销毁会话（登出）
     *
     * @param sessionId 会话ID
     */
    public void destroySession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }

        String key = SESSION_KEY_PREFIX + sessionId;
        redisService.delete(key);
        log.info("会话已销毁，sessionId：{}", sessionId);
    }

    /**
     * 续期会话
     *
     * @param sessionId 会话ID
     */
    public void renewSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }

        String key = SESSION_KEY_PREFIX + sessionId;
        if (redisService.exists(key)) {
            redisService.expire(key, Duration.ofSeconds(properties.getAuth().getSessionExpiration()));
        }
    }

    /**
     * Session 数据结构
     */
    @Data
    public static class SessionData implements Serializable {
        private String username;
        private Long loginTime;
    }
}
