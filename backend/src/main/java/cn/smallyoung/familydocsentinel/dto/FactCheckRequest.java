package cn.smallyoung.familydocsentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建流言核验任务请求参数 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactCheckRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户粘贴的待核验健康文本（上限 2000 字符）
     */
    private String text;

    /**
     * 用户是否点击了“这只是文章里的内容（继续核验）”
     * <p>
     * 对应需求 3.3 与 4.1 节：当值为 true 时，即使服务端扫描命中 HARD 拦截级红线，
     * 也会将其降级为 WARN 提示级并继续后续核验流程。
     * </p>
     */
    private boolean ignoreHardRedFlag;

    /**
     * 是否跳过服务端高频缓存并强制重新核验（对应需求 US-09“重新核验”）
     */
    private boolean skipCache;
}

