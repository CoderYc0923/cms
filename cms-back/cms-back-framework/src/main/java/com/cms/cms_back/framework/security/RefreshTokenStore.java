package com.cms.cms_back.framework.security;

import java.util.Optional;
import java.util.UUID;

// StringRedisTemplate 操作字符串类型的数据，使用字符串作为key，value为字符串
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.shaded.gson.JsonParseException;

import lombok.Data;
import tools.jackson.databind.ObjectMapper;

@Component
public class RefreshTokenStore {

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redis;
    private final CmsSecurityProperties properties;

    private static final String KEY_PREFIX = "cms:auth:refresh:";

    public RefreshTokenStore(ObjectMapper objectMapper, StringRedisTemplate redis, CmsSecurityProperties properties) {
        this.objectMapper = objectMapper;
        this.redis = redis;
        this.properties = properties;
    }

    /**
     * 刷新令牌会话用户信息
     * SessionUser
     */
    @Data
    public static class SessionUser {
        private Long userId;
        private String username;

        public SessionUser(Long userId, String username) {
            this.userId = userId;
            this.username = username;
        }
    }

    /**
     * 颁发刷新令牌
     * @param userId 用户ID
     * @param username 用户名
     * @return 刷新令牌
     */
    public String issue(Long userId, String username) {
        /**两次UUID拼接，避免长度不够 */
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        try {
            String json = objectMapper.writeValueAsString(new SessionUser(userId, username));
            redis.opsForValue().set(KEY_PREFIX + token, json, properties.getRefreshTokenTtl());
            return token;
        } catch (JsonParseException e) {
            throw new IllegalStateException("serialize refresh session failed", e);
        }
    }

    /**
     * 根据刷新令牌查询会话用户信息
     * @param refreshToken 刷新令牌
     * @return 会话用户信息
     */
    public Optional<SessionUser> find(String refreshToken) {
        String key = KEY_PREFIX + refreshToken;
        String json = redis.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, SessionUser.class));
        } catch (JsonParseException e) {
            return Optional.empty();
        }
    }

    /**
     * 撤销刷新令牌
     * @param refreshToken 刷新令牌
     */
    public void revoke(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            redis.delete(KEY_PREFIX + refreshToken);
        }
    }
}
