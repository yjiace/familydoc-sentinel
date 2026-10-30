package cn.smallyoung.familydocsentinel.service;

import cn.hutool.v7.core.text.StrUtil;
import cn.smallyoung.familydocsentinel.dto.*;
import cn.smallyoung.familydocsentinel.entity.UserEntity;
import cn.smallyoung.familydocsentinel.entity.UserUsageStatEntity;
import cn.smallyoung.familydocsentinel.exception.BizException;
import cn.smallyoung.familydocsentinel.repository.UserRepository;
import cn.smallyoung.familydocsentinel.repository.UserUsageStatRepository;
import cn.smallyoung.familydocsentinel.security.JwtTokenProvider;
import cn.smallyoung.familydocsentinel.security.PasswordHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户体系业务服务
 */
@Slf4j
@Service
public class UserService {

    @Resource
    private UserRepository userRepository;

    @Resource
    private UserUsageStatRepository userUsageStatRepository;

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private JwtTokenProvider jwtTokenProvider;

    @Resource
    private CaptchaService captchaService;

    /**
     * 邮箱+验证码+密码注册
     */
    @Transactional(rollbackFor = Exception.class)
    public UserAuthResponse register(UserRegisterRequest request) {
        if (request == null || StrUtil.isBlank(request.getEmail()) || StrUtil.isBlank(request.getPassword())) {
            throw BizException.of("邮箱和密码不能为空");
        }
        String email = request.getEmail().trim().toLowerCase();
        if (!email.contains("@")) {
            throw BizException.of("邮箱格式不正确");
        }
        if (request.getPassword().length() < 6) {
            throw BizException.of("密码长度至少为 6 位");
        }

        // 1. 校验验证码
        emailCodeService.verifyAndConsumeCode(email, "REGISTER", request.getCode());

        // 2. 检查邮箱唯一性
        if (userRepository.existsByEmail(email)) {
            throw BizException.of("该邮箱已被注册，请直接登录");
        }

        // 3. 生成昵称（未填写时默认使用邮箱前缀）
        String nickname = request.getNickname();
        if (StrUtil.isBlank(nickname)) {
            nickname = email.substring(0, email.indexOf('@'));
        }

        // 4. 密码加盐哈希并保存
        String encodedPassword = PasswordHelper.encode(request.getPassword().trim());
        UserEntity user = UserEntity.builder()
                .email(email)
                .password(encodedPassword)
                .nickname(nickname)
                .role("USER")
                .status("ACTIVE")
                .build();
        user = userRepository.save(user);

        // 5. 初始化用户资源用量统计记录
        UserUsageStatEntity usageStat = UserUsageStatEntity.builder()
                .userId(user.getId())
                .totalPromptTokens(0L)
                .totalCompletionTokens(0L)
                .totalTokens(0L)
                .totalSearchCount(0L)
                .taskCount(0)
                .build();
        userUsageStatRepository.save(usageStat);

        // 6. 签发 Token
        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail(), user.getRole());

        return UserAuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .build();
    }

    /**
     * 邮箱+密码登录
     */
    public UserAuthResponse login(UserLoginRequest request) {
        if (request == null || StrUtil.isBlank(request.getEmail()) || StrUtil.isBlank(request.getPassword())) {
            throw BizException.of("邮箱和密码不能为空");
        }

        // 校验图形验证码
        captchaService.verifyCaptcha(request.getCaptchaKey(), request.getCaptchaCode());

        String email = request.getEmail().trim().toLowerCase();

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> BizException.of("邮箱或密码错误"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw BizException.of("账号已被停用，请联系管理员");
        }

        if (!PasswordHelper.matches(request.getPassword().trim(), user.getPassword())) {
            throw BizException.of("邮箱或密码错误");
        }

        String token = jwtTokenProvider.createToken(user.getId(), user.getEmail(), user.getRole());

        return UserAuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .build();
    }

    /**
     * 已登录用户修改密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (userId == null) {
            throw BizException.of("用户未登录");
        }
        if (request == null || StrUtil.isBlank(request.getOldPassword()) || StrUtil.isBlank(request.getNewPassword())) {
            throw BizException.of("原密码和新密码不能为空");
        }
        if (request.getNewPassword().length() < 6) {
            throw BizException.of("新密码长度至少为 6 位");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> BizException.of("用户不存在"));

        if (!PasswordHelper.matches(request.getOldPassword().trim(), user.getPassword())) {
            throw BizException.of("原密码输入错误");
        }

        user.setPassword(PasswordHelper.encode(request.getNewPassword().trim()));
        userRepository.save(user);
    }

    /**
     * 通过验证码重置密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordRequest request) {
        if (request == null || StrUtil.isBlank(request.getEmail()) || StrUtil.isBlank(request.getNewPassword())) {
            throw BizException.of("邮箱和新密码不能为空");
        }
        String email = request.getEmail().trim().toLowerCase();
        if (request.getNewPassword().length() < 6) {
            throw BizException.of("新密码长度至少为 6 位");
        }

        // 1. 验证验证码
        emailCodeService.verifyAndConsumeCode(email, "RESET_PWD", request.getCode());

        // 2. 查询用户
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> BizException.of("该邮箱未注册"));

        // 3. 更新密码
        user.setPassword(PasswordHelper.encode(request.getNewPassword().trim()));
        userRepository.save(user);
    }

    /**
     * 查询用户个人资料
     */
    public UserProfileDto getProfile(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> BizException.of("用户不存在"));

        return UserProfileDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * 更新用户资料（昵称、头像）
     */
    @Transactional(rollbackFor = Exception.class)
    public UserProfileDto updateProfile(Long userId, String nickname, String avatar) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> BizException.of("用户不存在"));

        if (StrUtil.isNotBlank(nickname)) {
            user.setNickname(nickname.trim());
        }
        if (avatar != null) {
            user.setAvatar(avatar.trim());
        }
        user = userRepository.save(user);

        return getProfile(userId);
    }
}
