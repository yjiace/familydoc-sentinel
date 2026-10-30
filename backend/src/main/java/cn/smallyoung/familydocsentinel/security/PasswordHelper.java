package cn.smallyoung.familydocsentinel.security;

import cn.hutool.v7.crypto.digest.BCrypt;

/**
 * 密码加盐哈希与校验工具
 */
public final class PasswordHelper {

    private PasswordHelper() {
    }

    /**
     * 对原始明文密码进行 BCrypt 加盐哈希
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(10));
    }

    /**
     * 比对明文密码与哈希密文是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, encodedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}

