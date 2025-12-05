package com.lytoyo.common.utils;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Package:com.lytoyo.common.utils
 *
 * @ClassName:PasswordUtil
 * @Create:2025/12/1 9:25
 **/
@Component
public class PasswordUtil {
    /**
     * 生成随机盐值
     */
    public String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 加密密码（密码+盐值）
     */
    public String encryptPassword(String password, String salt) {
        try {
            // 使用PBKDF2算法
            KeySpec spec = new PBEKeySpec(password.toCharArray(),
                    salt.getBytes(), 10000, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }

    /**
     * 验证密码
     */
    public boolean verifyPassword(String inputPassword, String storedPassword, String salt) {
        String encryptedInput = encryptPassword(inputPassword, salt);
        return encryptedInput.equals(storedPassword);
    }
}
