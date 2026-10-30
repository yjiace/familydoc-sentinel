package cn.smallyoung.familydocsentinel.security;

import cn.smallyoung.familydocsentinel.exception.BizException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户认证与权限拦截器
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Resource
    private JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = resolveToken(request);
        Long userId = null;
        if (token != null) {
            userId = jwtTokenProvider.validateAndGetUserId(token);
            if (userId != null) {
                UserContext.setUserId(userId);
            }
        }

        // 如果不是 Controller 方法（如资源放行），直接通过
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 检查是否包含 @RequireAuth 注解
        boolean requireAuth = handlerMethod.hasMethodAnnotation(RequireAuth.class)
                || handlerMethod.getBeanType().isAnnotationPresent(RequireAuth.class);

        if (requireAuth && userId == null) {
            throw BizException.of(401, "登录已失效或尚未登录，请先登录");
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}

