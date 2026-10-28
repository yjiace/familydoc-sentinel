package cn.smallyoung.familydocsentinel.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 危急红线扫描结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedFlagResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 级别: HARD (拦截级), WARN (提示级), NONE (未命中)
     */
    private Level level;

    /**
     * 类别: BP (血压), GLUCOSE (血糖), SYMPTOM (突发急重症)
     */
    private Category category;

    /**
     * 警报文案
     */
    private String message;

    /** 未命中 */
    public static RedFlagResult none() {
        return RedFlagResult.builder().level(Level.NONE).build();
    }

    /** 命中 */
    public static RedFlagResult of(Level level, Category category, String message) {
        return RedFlagResult.builder()
                .level(level)
                .category(category)
                .message(message)
                .build();
    }

    /** 是否需要拦截（不再进入医疗分析） */
    @JsonIgnore
    public boolean isBlocked() {
        return level == Level.HARD;
    }

    /** 是否命中任意红线（HARD） */
    @JsonIgnore
    public boolean isHit() {
        return level == Level.HARD;
    }

    public enum Level {
        NONE("none"), WARN("warn"), HARD("hard");

        private final String code;

        Level(String code) {
            this.code = code;
        }

        /** JSON 序列化沿用原来的小写字符串，前端无需改动 */
        @JsonValue
        public String code() {
            return code;
        }
    }

    public enum Category {
        BP("bp"), GLUCOSE("glucose"), SYMPTOM("symptom");

        private final String code;

        Category(String code) {
            this.code = code;
        }

        @JsonValue
        public String code() {
            return code;
        }
    }
}