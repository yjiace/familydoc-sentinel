package cn.smallyoung.familydocsentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送邮箱验证码请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendCodeRequest {

    /**
     * 接收验证码的邮箱
     */
    private String email;

    /**
     * 业务类型：REGISTER（注册）、RESET_PWD（重置密码）
     */
    private String bizType;
}

