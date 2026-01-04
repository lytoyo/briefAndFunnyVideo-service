package com.lytoyo.common.utils;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机数生成工具类 - 完整功能版本
 */
public final class CodeUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String NUMBERS = "0123456789";
    private static final int SIX_DIGIT_MIN = 100000;
    private static final int SIX_DIGIT_MAX = 999999;

    private CodeUtil() {
        // 工具类，防止实例化
        throw new UnsupportedOperationException("工具类不能实例化");
    }

    /**
     * 生成随机6位数字（高性能）
     */
    public static String generateSixDigitCode() {
        return generateSixDigitCode(false);
    }

    /**
     * 生成随机6位数字
     * @param secure 是否使用安全随机数生成器
     * @return 6位数字字符串
     */
    public static String generateSixDigitCode(boolean secure) {
        int code;
        if (secure) {
            code = SECURE_RANDOM.nextInt(SIX_DIGIT_MAX - SIX_DIGIT_MIN + 1) + SIX_DIGIT_MIN;
        } else {
            code = ThreadLocalRandom.current().nextInt(SIX_DIGIT_MIN, SIX_DIGIT_MAX + 1);
        }
        return String.valueOf(code);
    }

    /**
     * 生成指定位数的数字验证码
     * @param length 验证码长度
     * @param secure 是否使用安全随机数
     * @return 数字验证码字符串
     */
    public static String generateDigitCode(int length, boolean secure) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }

        if (length == 1) {
            return secure ?
                    String.valueOf(SECURE_RANDOM.nextInt(10)) :
                    String.valueOf(ThreadLocalRandom.current().nextInt(10));
        }

        int min = (int) Math.pow(10, length - 1);
        int max = (int) Math.pow(10, length) - 1;

        String code = secure ?
                String.valueOf(SECURE_RANDOM.nextInt(max - min + 1) + min) :
                String.valueOf(ThreadLocalRandom.current().nextInt(min, max + 1));

        return code;
    }

    /**
     * 生成随机数字字符串（可能包含前导零）
     */
    public static String generateDigitString(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int digit = ThreadLocalRandom.current().nextInt(10);
            sb.append(digit);
        }
        return sb.toString();
    }

    /**
     * 批量生成随机6位数字
     */
    public static String[] generateSixDigitCodes(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("数量必须大于0");
        }

        String[] codes = new String[count];
        for (int i = 0; i < count; i++) {
            codes[i] = generateSixDigitCode();
        }
        return codes;
    }
}