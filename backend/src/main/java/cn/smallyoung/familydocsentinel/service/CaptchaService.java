package cn.smallyoung.familydocsentinel.service;

import cn.hutool.v7.core.data.id.IdUtil;
import cn.hutool.v7.swing.captcha.CaptchaUtil;
import cn.hutool.v7.swing.captcha.LineCaptcha;
import cn.hutool.v7.swing.captcha.generator.RandomGenerator;
import cn.smallyoung.familydocsentinel.exception.BizException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务（基于 Hutool 生成纯数字图形验证码，优先存储 Redis，支持优雅内存降级）
 */
@Slf4j
@Service
public class CaptchaService {

    private static final String CAPTCHA_KEY_PREFIX = "auth:captcha:";
    private static final int EXPIRE_MINUTES = 3;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 当 Redis 出现连接异常时的本地内存兜底缓存
     */
    private final Cache<String, String> localCaptchaCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(EXPIRE_MINUTES))
            .maximumSize(5000)
            .build();

    /**
     * 生成 4 位纯数字图形验证码
     *
     * @return 包含 captchaKey 与 imageBase64 (Data URL 格式) 的 Map
     */
    public Map<String, String> generateCaptcha() {
        // 宽度 115，高度 38，4 位字符，15 条干扰线
        LineCaptcha lineCaptcha = CaptchaUtil.ofLineCaptcha(115, 38, 4, 15);
        // 全部为数字
        lineCaptcha.setGenerator(new RandomGenerator("0123456789", 4));
        lineCaptcha.createCode();

        String code = lineCaptcha.getCode();
        String imageBase64 = lineCaptcha.getImageBase64Data();
        String captchaKey = IdUtil.fastSimpleUUID();

        // 存储至 Redis
        boolean storedInRedis = false;
        try {
            if (stringRedisTemplate != null) {
                stringRedisTemplate.opsForValue().set(
                        CAPTCHA_KEY_PREFIX + captchaKey,
                        code,
                        EXPIRE_MINUTES,
                        TimeUnit.MINUTES
                );
                storedInRedis = true;
            }
        } catch (Exception e) {
            log.warn("Redis 存储图形验证码失败，降级使用内存缓存: {}", e.getMessage());
        }

        if (!storedInRedis) {
            localCaptchaCache.put(captchaKey, code);
        }

        return Map.of(
                "captchaKey", captchaKey,
                "imageBase64", imageBase64
        );
    }

    /**
     * 校验并立即核销图形验证码
     *
     * @param captchaKey 验证码唯一识别 key
     * @param inputCode  用户输入的验证码
     */
    public void verifyCaptcha(String captchaKey, String inputCode) {
        if (captchaKey == null || captchaKey.isBlank() || inputCode == null || inputCode.isBlank()) {
            throw BizException.of("请输入图形验证码");
        }

        String targetCode = null;
        try {
            if (stringRedisTemplate != null) {
                targetCode = stringRedisTemplate.opsForValue().get(CAPTCHA_KEY_PREFIX + captchaKey);
                if (targetCode != null) {
                    stringRedisTemplate.delete(CAPTCHA_KEY_PREFIX + captchaKey);
                }
            }
        } catch (Exception e) {
            log.warn("Redis 读取图形验证码异常，尝试从本地缓存读取: {}", e.getMessage());
        }

        if (targetCode == null) {
            targetCode = localCaptchaCache.getIfPresent(captchaKey);
            if (targetCode != null) {
                localCaptchaCache.invalidate(captchaKey);
            }
        }

        if (targetCode == null) {
            throw BizException.of("图形验证码已失效，请点击刷新");
        }

        if (!targetCode.equalsIgnoreCase(inputCode.trim())) {
            throw BizException.of("图形验证码输入错误");
        }
    }
}

