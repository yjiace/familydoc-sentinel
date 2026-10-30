package cn.smallyoung.familydocsentinel.security;

import cn.hutool.v7.json.jwt.JWT;
import cn.hutool.v7.json.jwt.JWTUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token 签发与解析服务（基于 HMAC-SHA256）
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:familydoc-sentinel-secure-jwt-secret-key-2026-very-safe!}")
    private String secret;

    @Value("${jwt.expiration-hours:168}")
    private long expirationHours;

    /**
     * 为用户生成 JWT 令牌
     */
    public String createToken(Long userId, String email, String role) {
        long expireMillis = System.currentTimeMillis() + expirationHours * 3600 * 1000L;
        Date expireDate = new Date(expireMillis);

        Map<String, Object> payloads = new HashMap<>();
        payloads.put("userId", userId);
        payloads.put("email", email);
        payloads.put("role", role);
        payloads.put("exp", expireDate.getTime() / 1000);

        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        return JWTUtil.createToken(payloads, key);
    }

    /**
     * 校验并解析 Token 中的 userId，非法或过期返回 null
     */
    public Long validateAndGetUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            byte[] key = secret.getBytes(StandardCharsets.UTF_8);
            JWT jwt = JWT.of(token);
            if (!jwt.setKey(key).verify()) {
                return null;
            }
            // 检查过期时间
            Object expObj = jwt.getPayload("exp");
            if (expObj != null) {
                long expSeconds = Long.parseLong(expObj.toString());
                if (System.currentTimeMillis() / 1000 > expSeconds) {
                    return null;
                }
            }
            Object userIdObj = jwt.getPayload("userId");
            if (userIdObj != null) {
                return Long.parseLong(userIdObj.toString());
            }
        } catch (Exception e) {
            log.debug("解析或验证 JWT 失败: {}", e.getMessage());
        }
        return null;
    }
}
