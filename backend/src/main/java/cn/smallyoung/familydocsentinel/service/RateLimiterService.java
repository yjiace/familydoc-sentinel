package cn.smallyoung.familydocsentinel.service;

import cn.smallyoung.familydocsentinel.utils.RequestUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

/**
 * IP 级别滑动窗口限流服务 (默认每 10 分钟最多 10 次)
 */
@Slf4j
@Service
public class RateLimiterService {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_NANOS = TimeUnit.MINUTES.toNanos(10);

    private final Cache<String, Deque<Long>> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    public boolean tryAcquire(HttpServletRequest request) {
        return tryAcquire(toKey(RequestUtils.getIp(request)));
    }

    boolean tryAcquire(String key) {
        Deque<Long> deque = buckets.get(key, k -> new ArrayDeque<>(MAX_REQUESTS));
        long now = System.nanoTime();
        synchronized (deque) {
            while (!deque.isEmpty() && now - deque.peekFirst() > WINDOW_NANOS) {
                deque.pollFirst();
            }
            if (deque.size() >= MAX_REQUESTS) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }

    /** IPv6 取前 4 段（/64），IPv4 原样返回 */
    private static String toKey(String ip) {
        if (ip.indexOf(':') < 0) {
            return ip;
        }
        String[] parts = ip.split(":");
        return parts.length >= 4 ? String.join(":", Arrays.copyOf(parts, 4)) : ip;
    }
}
