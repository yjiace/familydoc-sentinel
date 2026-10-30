package cn.smallyoung.familydocsentinel.security;

import java.util.Optional;

/**
 * 当前请求用户上下文管理（基于 ThreadLocal 线程隔离）
 */
public final class UserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Long getUserId() {
        return CURRENT_USER_ID.get();
    }

    public static Optional<Long> getOptionalUserId() {
        return Optional.ofNullable(CURRENT_USER_ID.get());
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}

