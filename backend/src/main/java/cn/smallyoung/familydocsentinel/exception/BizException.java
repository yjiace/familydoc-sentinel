package cn.smallyoung.familydocsentinel.exception;

import cn.hutool.v7.core.text.StrUtil;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常
 * <p>
 * - code: 业务错误码，为 null 时全局处理器默认使用 500
 * - message: 直接通过父类 {@link RuntimeException#getMessage()} 获取，不重复存储
 * </p>
 *
 * @author smallyoung
 */
@Getter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 6868285969639994913L;

    /**
     * 业务错误码（可选），为 null 时全局异常处理使用 HTTP 500
     */
    private final Integer code;

    public BizException(String msg) {
        super(msg);
        this.code = null;
    }

    public BizException(String msg, Throwable cause) {
        super(msg, cause);
        this.code = null;
    }

    public BizException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }

    public BizException(Integer code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
    }

    // ---- 静态工厂（可选，便于调用方使用链式风格）----

    public static BizException of(String msg) {
        return new BizException(msg);
    }

    public static BizException of(String template, Object... args) {
        return new BizException(StrUtil.format(template, args));
    }

    public static BizException of(Integer code, String msg) {
        return new BizException(code, msg);
    }

    public static BizException of(Integer code, String template, Object... args) {
        return new BizException(code, StrUtil.format(template, args));
    }
}
