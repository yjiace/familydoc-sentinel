package cn.smallyoung.familydocsentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户认证响应（Token与基本信息）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthResponse {

    private String token;

    private Long userId;

    private String email;

    private String nickname;

    private String avatar;

    private String role;
}

