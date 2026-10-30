package cn.smallyoung.familydocsentinel.security;

import java.lang.annotation.*;

/**
 * 标明需要登录认证的 Controller 或方法
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAuth {
}

