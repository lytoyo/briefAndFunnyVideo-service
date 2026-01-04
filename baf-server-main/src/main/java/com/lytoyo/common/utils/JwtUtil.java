package com.lytoyo.common.utils;

import com.lytoyo.common.constant.SystemConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    /**
     * 过期时间，7天
     */
    private static final long EXPIRE = SystemConstant.TOKENEXPIRE;
    /**
     * 加密密钥
     */
    private static final String SECRET = SystemConstant.TOKENSECRET;
    /**
     * 使用HS256算法生成密钥
     */
    private static final SecretKey KEY = Jwts.SIG.HS256.key().random(new SecureRandom(SECRET.getBytes())).build();
    /**
     * 主题
     */
    private static final String SUBJECT = SystemConstant.TOKENSUBJECT;

    /**
     * 根据用户id信息生成令牌
     *
     * @param id
     * @return
     */
    public static String geneJsonWebToken(Long id) {
        Map<String, Object> claims = new HashMap<>();
        // 将 Long 转换为字符串存储，避免精度丢失
        claims.put("id", id.toString());

        String token = Jwts.builder()
                .claims(claims)
                .subject(SUBJECT)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(KEY)
                .compact();
        return token;
    }

    /**
     * 验证令牌是否有效
     *
     * @param token
     * @return
     */
    public static Long checkJWT(String token) {

        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token);

            Claims claims = jws.getPayload();
            Object idObj = claims.get("id");

            // 由于存储为字符串，直接解析
            if (idObj instanceof String) {
                return Long.valueOf((String) idObj);
            } else if (idObj != null) {
                // 兼容处理
                return Long.valueOf(idObj.toString());
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}