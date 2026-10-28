package cn.smallyoung.familydocsentinel.chain.pipeline;

import cn.smallyoung.familydocsentinel.enums.TaskStage;
import cn.smallyoung.familydocsentinel.exception.BizException;
import cn.smallyoung.familydocsentinel.service.ContentSanitizerService;
import cn.smallyoung.familydocsentinel.service.WsaSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 主流程责任链节点 1（@Order 10）：输入校验、预处理清洗与日度额度检查（对应需求文档 US-01、3.1、5 节）
 * <p>
 * 职责：
 * 1. 去除首尾空白，合并连续空行；
 * 2. 校验输入为空或超过 2000 字时立即阻断并提示“请输入 2000 字以内的文字”（不截断，保留原文）；
 * 3. 检查今日全局检索额度是否耗尽；
 * 4. 生成清洗文本（cleanText）与规范化全文 SHA-256 指纹（queryHash）。
 * </p>
 */
@Slf4j
@Order(10)
@Component
@RequiredArgsConstructor
public class InputSanitizeHandler implements FactCheckPipelineHandler {

    private static final int MAX_INPUT_CHARS = 2000;
    private static final String LENGTH_EXCEEDED_MSG = "请输入 2000 字以内的文字";

    private final ContentSanitizerService contentSanitizerService;
    private final WsaSearchService wsaSearchService;

    @Override
    public boolean isSynchronousPreCheck() {
        return true;
    }

    @Override
    public boolean handle(FactCheckContext context) {
        context.updateStage(TaskStage.SANITIZING);

        String raw = context.getRawInput();
        if (raw == null || raw.isBlank()) {
            throw BizException.of(LENGTH_EXCEEDED_MSG);
        }

        // 先去除首尾空白后检查 2000 字上限（需求 US-01 & 第 5 节：输入为空或超 2000 字提示“请输入 2000 字以内的文字”）
        String trimmed = raw.strip();
        if (trimmed.isEmpty() || trimmed.length() > MAX_INPUT_CHARS) {
            throw BizException.of(LENGTH_EXCEEDED_MSG);
        }

        // 检查全局每日核验检索额度是否可用（需求 4.10 & 第 5 节）
        wsaSearchService.checkDailyQuotaAvailable();

        // 执行文本清洗、合并连续空行与 SHA-256 哈希计算
        ContentSanitizerService.SanitizedContent sanitized = contentSanitizerService.sanitize(trimmed);
        if (sanitized.cleanText().isBlank()) {
            throw BizException.of(LENGTH_EXCEEDED_MSG);
        }

        context.setNormalizedRawText(sanitized.normalizedRawText());
        context.setCleanText(sanitized.cleanText());
        context.setQueryHash(sanitized.queryHash());
        context.getResponse().setQueryHash(sanitized.queryHash());
        return true;
    }
}

