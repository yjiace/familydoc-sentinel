package cn.smallyoung.familydocsentinel.controller;

import cn.smallyoung.familydocsentinel.dto.*;
import cn.smallyoung.familydocsentinel.interfaces.ResponseSysResult;
import cn.smallyoung.familydocsentinel.security.RequireAuth;
import cn.smallyoung.familydocsentinel.security.UserContext;
import cn.smallyoung.familydocsentinel.service.EmailCodeService;
import cn.smallyoung.familydocsentinel.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户认证与账号管理控制器
 */
@Slf4j
@RestController
@ResponseSysResult
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Resource
    private UserService userService;

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private cn.smallyoung.familydocsentinel.service.CaptchaService captchaService;

    /**
     * 获取纯数字图形验证码
     */
    @GetMapping("captcha")
    public Map<String, String> getCaptcha() {
        return captchaService.generateCaptcha();
    }

    /**
     * 发送邮箱验证码（支持注册与重置密码）
     */
    @PostMapping("send-code")
    public Map<String, Object> sendCode(@RequestBody SendCodeRequest request) {
        String bizType = request.getBizType() != null ? request.getBizType() : "REGISTER";
        emailCodeService.sendCode(request.getEmail(), bizType);
        return Map.of("success", true, "message", "验证码已发送至您的邮箱，请查收");
    }

    /**
     * 邮箱+验证码+密码注册
     */
    @PostMapping("register")
    public UserAuthResponse register(@RequestBody UserRegisterRequest request) {
        return userService.register(request);
    }

    /**
     * 邮箱+密码登录
     */
    @PostMapping("login")
    public UserAuthResponse login(@RequestBody UserLoginRequest request) {
        return userService.login(request);
    }

    /**
     * 通过验证码重置密码
     */
    @PostMapping("reset-password")
    public Map<String, Object> resetPassword(@RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request);
        return Map.of("success", true, "message", "密码重置成功，请使用新密码登录");
    }

    /**
     * 已登录用户修改密码
     */
    @RequireAuth
    @PostMapping("change-password")
    public Map<String, Object> changePassword(@RequestBody ChangePasswordRequest request) {
        Long userId = UserContext.getUserId();
        userService.changePassword(userId, request);
        return Map.of("success", true, "message", "密码修改成功，请妥善保管新密码");
    }

    /**
     * 获取当前登录用户信息
     */
    @RequireAuth
    @GetMapping("me")
    public UserProfileDto getCurrentUser() {
        Long userId = UserContext.getUserId();
        return userService.getProfile(userId);
    }

    /**
     * 更新当前用户个人资料
     */
    @RequireAuth
    @PutMapping("profile")
    public UserProfileDto updateProfile(@RequestBody Map<String, String> body) {
        Long userId = UserContext.getUserId();
        String nickname = body.get("nickname");
        String avatar = body.get("avatar");
        return userService.updateProfile(userId, nickname, avatar);
    }

    /**
     * 登出（客户端清除 Token 即可）
     */
    @PostMapping("logout")
    public Map<String, Object> logout() {
        return Map.of("success", true, "message", "已安全退出");
    }
}

