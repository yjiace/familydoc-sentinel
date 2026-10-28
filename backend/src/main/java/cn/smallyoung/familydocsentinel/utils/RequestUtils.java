package cn.smallyoung.familydocsentinel.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author smallyoung
 */
public class RequestUtils {

    private static final String UNKNOWN = "unknown";

    private RequestUtils() {}

    public static HttpServletRequest getHttpServletRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes sra ? sra.getRequest() : null;
    }

    /** 容器已经处理好可信代理，这里只负责读取并规范化 */
    public static String getIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (ip == null || ip.isBlank()) {
            return UNKNOWN;
        }
        return "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip) ? "127.0.0.1" : ip;
    }
}
