package cn.smallyoung.familydocsentinel.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 用户输入文本清洗、去噪与指纹归一化服务（对应需求文档 3.1、3.2 节）
 * <p>
 * 负责：
 * 1. 去除首尾空白，统一换行符并合并连续空行；
 * 2. 剔除不可见控制字符、营销转发引导语与重复标点符号；
 * 3. 基于 NFKC 归一化计算 SHA-256 指纹哈希（queryHash），用于缓存探测。
 * </p>
 */
@Service
public class ContentSanitizerService {

    /**
     * 最大安全处理长度上限
     */
    private static final int MAX_LEN = 5000;

    /**
     * 零宽字符与不可见控制字符正则（保留回车、换行与制表符）
     */
    private static final Pattern INVISIBLE = Pattern
            .compile("[\\u200B-\\u200F\\u202A-\\u202E\\u2060\\uFEFF\\u00AD]|[\\p{Cc}&&[^\\r\\n\\t]]");

    /**
     * 连续空行正则（将多个连续换行合并为单个换行）
     */
    private static final Pattern CONSECUTIVE_BLANK_LINES = Pattern.compile("(\\r?\\n[ \\t\\u3000]*){2,}");

    /**
     * 单行内连续空白字符（不含换行符）
     */
    private static final Pattern HORIZONTAL_WS = Pattern.compile("[ \\t\\x0B\\f\\u3000]+");

    /**
     * 微信群/朋友圈常见震惊体标题标签
     */
    private static final Pattern TAG = Pattern.compile(
            "【\\s*(紧急通知|速看|曝光|震惊|转发|重要提醒|必看)[^】]{0,10}】"
                    + "|[（(]转自[^）)]{0,20}[）)]");

    /**
     * 常见转发诱导噪音词
     */
    private static final Pattern FORWARD_NOISE = Pattern.compile(
            "快转给身边(的)?(长辈|亲朋好友|父母|朋友)|转给相亲相爱一家人|速看马上要删|快收藏|看后立即转发|赶紧告诉家里人|一定要转给");

    /**
     * 连续重复标点符号折叠
     */
    private static final Pattern REPEATED = Pattern.compile("([?!？！。，,~～、])\\1+");

    /**
     * 用于生成全文缓存哈希的剥离规则（忽略标点、特殊符号与空白差异）
     */
    private static final Pattern FINGERPRINT_STRIP = Pattern.compile("[\\p{P}\\p{S}\\s]+");

    /**
     * 文本预处理与清洗结果记录
     *
     * @param normalizedRawText 去除首尾空白并合并连续空行后的规范化原文
     * @param cleanText         进一步剔除转发噪声与多余标点后的净文本
     * @param queryHash         规范化全文 SHA-256 哈希值
     */
    public record SanitizedContent(String normalizedRawText, String cleanText, String queryHash) {
        /**
         * 兼容旧方法名获取原文
         */
        public String rawText() {
            return normalizedRawText;
        }
    }

    /**
     * 执行输入预处理与归一化清洗
     *
     * @param rawInput 原始输入字符串
     * @return 清洗后的结构化对象
     */
    public SanitizedContent sanitize(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return new SanitizedContent("", "", "");
        }

        // 1. 截断极端超长输入并去除首尾空白
        String raw = rawInput.length() > MAX_LEN ? rawInput.substring(0, MAX_LEN) : rawInput;
        raw = raw.strip();

        // 2. 剔除不可见字符，统一换行符并合并连续空行（需求 3.1）
        String normalizedRaw = INVISIBLE.matcher(raw).replaceAll("");
        normalizedRaw = normalizedRaw.replace("\r\n", "\n").replace('\r', '\n');
        normalizedRaw = CONSECUTIVE_BLANK_LINES.matcher(normalizedRaw).replaceAll("\n").strip();

        // 3. 去除营销标签、转发套话与重复标点
        String text = TAG.matcher(normalizedRaw).replaceAll(" ");
        text = FORWARD_NOISE.matcher(text).replaceAll(" ");
        text = REPEATED.matcher(text).replaceAll("$1");
        text = HORIZONTAL_WS.matcher(text).replaceAll(" ").strip();

        // 若过度清洗导致有效正文不足，回退使用规范化原文
        if (text.length() < Math.min(4, normalizedRaw.length())) {
            text = normalizedRaw;
        }

        // 4. 计算与标点/空白无关的 NFKC 归一化全文 SHA-256 指纹
        String fp = FINGERPRINT_STRIP.matcher(
                Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)).replaceAll("");

        if (fp.isEmpty()) {
            fp = Normalizer.normalize(normalizedRaw, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        }

        return new SanitizedContent(normalizedRaw, text, sha256(fp));
    }

    private static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }
}
