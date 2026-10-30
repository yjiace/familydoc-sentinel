package cn.smallyoung.familydocsentinel.service;

import cn.hutool.v7.core.util.RandomUtil;
import cn.smallyoung.familydocsentinel.exception.BizException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码管理服务（基于 Redis 进行分布式缓存与 60s 限流，支持内存优雅降级，不再落库数据库）
 */
@Slf4j
@Service
public class EmailCodeService {

    private static final int CODE_EXPIRE_MINUTES = 5;
    private static final int SEND_INTERVAL_SECONDS = 60;
    private static final String CODE_KEY_PREFIX = "auth:email_code:";
    private static final String RATE_KEY_PREFIX = "auth:email_rate:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private EmailService emailService;

    /**
     * 降级本地内存验证码缓存
     */
    private final Cache<String, String> localCodeCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(CODE_EXPIRE_MINUTES))
            .maximumSize(5000)
            .build();

    /**
     * 降级本地 60 秒限流缓存
     */
    private final Cache<String, Long> localRateCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(SEND_INTERVAL_SECONDS))
            .maximumSize(5000)
            .build();

    /**
     * 发送邮箱验证码（含 60 秒防刷限流）
     *
     * @param email   目标邮箱
     * @param bizType 业务类型（REGISTER / RESET_PWD 等）
     */
    public void sendCode(String email, String bizType) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw BizException.of("邮箱格式不正确");
        }
        String normalizedEmail = email.trim().toLowerCase();
        String type = bizType != null ? bizType.toUpperCase() : "REGISTER";
        String rateKey = RATE_KEY_PREFIX + type + ":" + normalizedEmail;
        String codeKey = CODE_KEY_PREFIX + type + ":" + normalizedEmail;

        // 1. 检查 60 秒限流
        boolean isRateLimited = false;
        try {
            if (stringRedisTemplate != null) {
                Boolean hasKey = stringRedisTemplate.hasKey(rateKey);
                if (Boolean.TRUE.equals(hasKey)) {
                    isRateLimited = true;
                }
            }
        } catch (Exception e) {
            log.warn("Redis 限流检查异常，转用本地限流: {}", e.getMessage());
            if (localRateCache.getIfPresent(rateKey) != null) {
                isRateLimited = true;
            }
        }

        if (isRateLimited) {
            throw BizException.of("验证码发送太频繁，请稍后再试");
        }

        // 2. 生成 6 位随机纯数字验证码
        String code = RandomUtil.randomNumbers(6);

        // 3. 存储至 Redis 并设置过期时间与限流标记
        boolean storedInRedis = false;
        try {
            if (stringRedisTemplate != null) {
                stringRedisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
                stringRedisTemplate.opsForValue().set(rateKey, "1", SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);
                storedInRedis = true;
            }
        } catch (Exception e) {
            log.warn("Redis 存储邮箱验证码异常，降级使用内存存储: {}", e.getMessage());
        }

        if (!storedInRedis) {
            localCodeCache.put(codeKey, code);
            localRateCache.put(rateKey, System.currentTimeMillis());
        }

        // 4. 发送邮件
        String bizName = "REGISTER".equalsIgnoreCase(type) ? "用户注册" : "重置密码";
        emailService.sendVerificationCode(normalizedEmail, code, bizName);
    }

    /**
     * 校验并核销验证码
     *
     * @param email   邮箱
     * @param bizType 业务类型
     * @param code    输入的验证码
     */
    public void verifyAndConsumeCode(String email, String bizType, String code) {
        if (email == null || code == null || code.isBlank()) {
            throw BizException.of("验证码不能为空");
        }
        String normalizedEmail = email.trim().toLowerCase();
        String type = bizType != null ? bizType.toUpperCase() : "REGISTER";
        String codeKey = CODE_KEY_PREFIX + type + ":" + normalizedEmail;

        String targetCode = null;
        try {
            if (stringRedisTemplate != null) {
                targetCode = stringRedisTemplate.opsForValue().get(codeKey);
                if (targetCode != null) {
                    stringRedisTemplate.delete(codeKey);
                }
            }
        } catch (Exception e) {
            log.warn("Redis 读取邮箱验证码异常，尝试从本地缓存读取: {}", e.getMessage());
        }

        if (targetCode == null) {
            targetCode = localCodeCache.getIfPresent(codeKey);
            if (targetCode != null) {
                localCodeCache.invalidate(codeKey);
            }
        }

        if (targetCode == null) {
            throw BizException.of("验证码不存在或已过期，请重新获取");
        }

        if (!targetCode.equalsIgnoreCase(code.trim())) {
            throw BizException.of("邮箱验证码不正确");
        }
    }
}
