package cn.smallyoung.familydocsentinel.component;

import cn.smallyoung.familydocsentinel.exception.BizException;
import cn.smallyoung.familydocsentinel.interfaces.ResponseSysResult;
import cn.smallyoung.familydocsentinel.utils.SysResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.ObjectMapper;

import java.lang.annotation.Annotation;
import java.time.LocalDateTime;

/**
 * 统一响应封装 & 全局异常处理
 * <p>
 * 使用方式：在 Controller 类或方法上标注 {@link ResponseSysResult}，
 * 返回内容将被自动封装到 {@link SysResult} 中。
 * </p>
 *
 * @author smallyoung
 */
@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class ResponseSysResultAdvice implements ResponseBodyAdvice<Object> {

    private static final Class<? extends Annotation> ANNOTATION_TYPE = ResponseSysResult.class;

    /**
     * 注入 Spring 容器中的 ObjectMapper 单例，避免重复创建
     */
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:}")
    private String applicationName;

    @Override
    public boolean supports(MethodParameter returnType,
            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), ANNOTATION_TYPE)
                || returnType.hasMethodAnnotation(ANNOTATION_TYPE);
    }

    @Override
    public Object beforeBodyWrite(Object body, @NonNull MethodParameter returnType,
            @NonNull MediaType selectedContentType,
            @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
            @NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response) {
        // 已是 SysResult，不需要再次封装
        if (body instanceof SysResult<?>) {
            return body;
        }
        // String 类型需要手动序列化：因为 Spring 会用 StringHttpMessageConverter 处理
        // 导致类型不匹配，必须返回 String 而非对象
        boolean isStringReturn = body instanceof String
                || (returnType.getMethod() != null
                        && String.class.equals(returnType.getMethod().getReturnType()));
        if (isStringReturn) {
            try {
                return objectMapper.writeValueAsString(SysResult.success(body));
            } catch (JacksonIOException ex) {
                log.error("[ResponseAdvice] String 响应序列化失败", ex);
                return body;
            }
        }
        return SysResult.success(body);
    }

    /**
     * 全局异常处理器
     * <p>
     * - {@link BizException}：业务异常，记录 WARN 日志，不输出堆栈<br>
     * - 其他异常：系统异常，记录 ERROR 并附完整序列
     * </p>
     */
    @ExceptionHandler(Exception.class)
    public SysResult<Void> handler(HttpServletRequest request, Exception ex) {
        if (ex instanceof BizException bizEx) {
            log.warn("[BizException] {} {} - code={}, msg={}",
                    request.getMethod(), request.getRequestURI(),
                    bizEx.getCode(), bizEx.getMessage());
            int code = bizEx.getCode() != null ? bizEx.getCode() : HttpStatus.INTERNAL_SERVER_ERROR.value();
            return SysResult.failure(code, bizEx.getMessage());
        }
        log.error("[Exception] {} {} - source={}, time={}",
                request.getMethod(), request.getRequestURI(),
                applicationName, LocalDateTime.now(), ex);
        return SysResult.failure(ex.getMessage());
    }
}
