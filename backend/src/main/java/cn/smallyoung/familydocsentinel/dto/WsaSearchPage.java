package cn.smallyoung.familydocsentinel.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WsaSearchPage implements Serializable {

    @Serial
    private static final long serialVersionUID = -2554896385191604437L;

    /**
     * 结果标题
     */
    private String title;

    /**
     * 内容发布时间
     */
    @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime date;

    /**
     * 内容发布源url
     */
    private String url;

    /**
     * 标准摘要
     */
    private String passage;

    /**
     * 动态摘要（极速版不返回该字段）
     */
    private String content;

    /**
     * 网站名称，部分不知名站点结果可能为空
     */
    private String site;

    /**
     * 相关性得分，取值0～1，越靠近1表示越相关
     */
    private Double score;

    /**
     * 权威度得分，取值0～5，数值越大表示越权威（仅旗舰版返回该字段）
     */
    @JsonAlias("authority_level")
    private String authorityLevel;

    /**
     * 图片列表，单个doc返回0～10条（极速版不返回该字段）
     */
    private List<Pic> pics;

    /**
     * 网站图标链接，部分不知名站点结果可能为空
     */
    private String favicon;

    /**
     * 附件子链信息，单个doc最多返回10条子链信息。（仅旗舰版返回该字段，通过Deeplinks入参控制）
     */
    private List<Deeplink> deeplinks;

    @Data
    public static class Pic implements Serializable {

        @Serial
        private static final long serialVersionUID = 4082411233932171160L;

        /**
         * 图片描述
         */
        private String caption;

        /**
         * 源图url地
         */
        @JsonAlias("origin_url")
        private String originUrl;

    }

    @Data
    public static class Deeplink implements Serializable {

        @Serial
        private static final long serialVersionUID = 3180657610313872827L;

        /**
         * 子链标题
         */
        private String title;

        /**
         * 子链地址
         */
        private String url;
    }
}
