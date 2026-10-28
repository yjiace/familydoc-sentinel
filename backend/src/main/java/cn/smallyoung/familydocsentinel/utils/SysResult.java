package cn.smallyoung.familydocsentinel.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应封装类
 *
 * <pre>
 * 成功：{code: 200, message: "OK", data: ...}
 * 失败：{code: 500, message: "...", data: null}
 * </pre>
 *
 * @param <T> 响应数据类型
 * @author smallyoung
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = -3937755350175676600L;

    private final Integer code;

    private final String message;

    private final T data;

    private SysResult(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ---- 静态工厂 ----

    public static <T> SysResult<T> result(HttpStatus status, T data) {
        return new SysResult<>(status.value(), status.getReasonPhrase(), data);
    }

    /**
     * 成功响应（HTTP 200）
     */
    public static <T> SysResult<T> success(T data) {
        return result(HttpStatus.OK, data);
    }

    /**
     * 成功响应，无数据
     */
    public static SysResult<Void> success() {
        return result(HttpStatus.OK, null);
    }

    /**
     * 失败响应（HTTP 500）
     */
    public static <T> SysResult<T> failure(String message) {
        return new SysResult<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, null);
    }

    /**
     * 失败响应（自定义状态码）
     */
    public static <T> SysResult<T> failure(Integer code, String message) {
        return new SysResult<>(code, message, null);
    }
}
