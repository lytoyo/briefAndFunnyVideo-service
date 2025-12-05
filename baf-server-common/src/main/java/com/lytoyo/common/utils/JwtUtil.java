package com.lytoyo.common.utils;

import com.lytoyo.common.constant.SystemConstant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
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
        Map<String, Long> claims = new HashMap<>();
        claims.put("id", id);
        String token = Jwts.builder().claims()
                .add(claims)  // 添加内容
                .subject(SUBJECT)   // 声明主题
                .issuedAt(new Date())   // 创建JWT时的时间戳
                .expiration(new Date(System.currentTimeMillis() + EXPIRE))  // 设置过期时间
                .and()  // 返回JwtBuilder配置
                .signWith(KEY)  // 签名
                .compact(); // 紧凑
        return token;
    }

    /**
     * 验证令牌是否有效
     *
     * @param token
     * @return
     */
    public static Long checkJWT(String token) {
        try{
            final Claims claims = Jwts.parser()
                    .verifyWith(KEY)    // 验证所有遇到的JWS签名
                    .build()
                    .parse(token).accept(Jws.CLAIMS)   // 解析jws
                    .getPayload();  // JWT有效载荷
            return claims.get("id",Long.class);
        }catch (Exception e){
            return null;
        }
    }
}