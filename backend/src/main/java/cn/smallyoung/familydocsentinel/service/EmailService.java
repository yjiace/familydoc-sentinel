package cn.smallyoung.familydocsentinel.service;

import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务
 */
@Slf4j
@Service
public class EmailService {

    @Resource(name = "mailSender")
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    /**
     * 发送邮箱验证码
     *
     * @param toEmail 收件人邮箱
     * @param code    6位验证码
     * @param bizName 业务名称（如“注册账户”、“重置密码”）
     */
    public void sendVerificationCode(String toEmail, String code, String bizName) {
        log.info("[邮箱验证码] 接收方: {}, 业务: {}, 验证码: {}", toEmail, bizName, code);

        if (mailUsername == null || mailUsername.isBlank()) {
            log.warn("未配置 spring.mail.username，进入调试降级模式。验证码: [{}]", code);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailUsername, "家医智核·流言粉碎");
            helper.setTo(toEmail);
            helper.setSubject("【家医智核】" + bizName + "验证码");

            String html = """
                    <div style="max-width: 560px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #ffffff;">
                        <div style="border-bottom: 2px solid #2563eb; padding-bottom: 12px; margin-bottom: 20px;">
                            <h2 style="margin: 0; color: #1e293b; font-size: 20px; display: flex; align-items: center;">
                                <span style="color: #2563eb;">家医智核</span>·流言粉碎
                            </h2>
                        </div>
                        <p style="color: #475569; font-size: 15px; line-height: 1.6;">您好！</p>
                        <p style="color: #475569; font-size: 15px; line-height: 1.6;">
                            您正在进行 <strong>%s</strong> 操作，您的邮箱验证码为：
                        </p>
                        <div style="background-color: #f1f5f9; padding: 16px; border-radius: 8px; text-align: center; margin: 24px 0;">
                            <span style="font-size: 30px; font-weight: 700; letter-spacing: 6px; color: #2563eb;">%s</span>
                        </div>
                        <p style="color: #64748b; font-size: 13px; line-height: 1.5;">
                            * 验证码有效期为 <strong>5 分钟</strong>。若非您本人操作，请忽略此邮件。
                        </p>
                        <div style="border-top: 1px solid #e2e8f0; margin-top: 24px; padding-top: 12px; color: #94a3b8; font-size: 12px; text-align: center;">
                            家医智核·流言粉碎平台 · 守护您和家人的健康
                        </div>
                    </div>
                    """.formatted(bizName, code);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("邮件验证码已成功投递至: {}", toEmail);
        } catch (Exception e) {
            log.error("发送邮件失败至: {}, 错误信息: {}", toEmail, e.getMessage());
            log.warn("虽然真实邮件投递失败，但验证码已在日志记录: [{}]", code);
        }
    }
}

