package cn.smallyoung.familydocsentinel.service;

import cn.smallyoung.familydocsentinel.exception.BizException;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.ocr.v20181119.OcrClient;
import com.tencentcloudapi.ocr.v20181119.models.GeneralBasicOCRRequest;
import com.tencentcloudapi.ocr.v20181119.models.GeneralBasicOCRResponse;
import com.tencentcloudapi.ocr.v20181119.models.TextDetection;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Set;

/**
 * 腾讯云通用文字识别服务
 * <p>
 * 支持微信群养生谣言截图、大字海报等图片的文本提取，无缝衔接后续核验流水线。
 * </p>
 */
@Slf4j
@Service
public class OcrService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "bmp");

    @Resource
    private OcrClient ocrClient;

    /**
     * 从上传的图片中识别并提取文字内容
     *
     * @param file 上传的图片文件
     * @return 提取并整理后的纯文本
     */
    public String extractTextFromImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.of("请上传待识别的图片文件");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw BizException.of("图片大小不能超过 5MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                throw BizException.of("仅支持 JPG、PNG、WEBP 或 BMP 格式的图片");
            }
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("读取上传图片字节失败", e);
            throw BizException.of("读取图片文件失败，请重新上传");
        }

        String base64Image = Base64.getEncoder().encodeToString(bytes);

        GeneralBasicOCRRequest req = new GeneralBasicOCRRequest();
        req.setImageBase64(base64Image);

        try {
            log.info("调用腾讯云通用印刷体识别 OCR, 文件名: {}, 大小: {} 字节", originalFilename, bytes.length);
            GeneralBasicOCRResponse resp = ocrClient.GeneralBasicOCR(req);

            TextDetection[] detections = resp.getTextDetections();
            if (detections == null || detections.length == 0) {
                throw BizException.of("未在图片中检测到清晰的文字内容，请上传包含文字的截图或海报");
            }

            StringBuilder sb = new StringBuilder();
            for (TextDetection detection : detections) {
                if (detection != null && detection.getDetectedText() != null) {
                    String line = detection.getDetectedText().trim();
                    if (!line.isEmpty()) {
                        sb.append(line).append("\n");
                    }
                }
            }

            String resultText = sb.toString().trim();
            if (resultText.isEmpty()) {
                throw BizException.of("未能成功提取图片文字，请尝试重新截图或直接粘贴文字");
            }

            log.info("腾讯云 OCR 识别完成, 提取文本字符数: {}", resultText.length());
            return resultText;

        } catch (TencentCloudSDKException e) {
            log.error("腾讯云 OCR SDK 调用异常: errorCode={}, message={}", e.getErrorCode(), e.getMessage(), e);
            String errorCode = e.getErrorCode() != null ? e.getErrorCode() : "";
            String message = e.getMessage() != null ? e.getMessage() : "";
            if (errorCode.contains("AuthFailure") || message.contains("AuthFailure")) {
                throw BizException.of("腾讯云 API 凭证校验失败，请联系管理员检查密钥配置");
            } else if (errorCode.contains("BalanceInsufficient") || errorCode.contains("ResourceUnavailable")
                    || message.contains("BalanceInsufficient") || message.contains("ResourceUnavailable")) {
                throw BizException.of("腾讯云 OCR 免费额度耗尽或服务未开通，请直接粘贴文本进行核验");
            } else if (errorCode.contains("LimitExceeded") || message.contains("LimitExceeded")) {
                throw BizException.of("识图请求过于频繁，请稍后再试");
            } else {
                throw BizException.of("图片文字识别失败: " + e.getMessage());
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("图片识别未知系统异常", e);
            throw BizException.of("图片文字识别异常，请稍后重试或直接粘贴文本");
        }
    }
}
