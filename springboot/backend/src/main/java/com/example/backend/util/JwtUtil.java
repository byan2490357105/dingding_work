package com.example.backend.util;

import com.example.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.UUID;

/**
 * JWT 工具类：负责生成与校验 Token。
 */
public class JwtUtil {

    /** 签名密钥（生产环境应放入配置文件） */
    private static final String SECRET = "ding-ding-work-jwt-secret";

    /** Token 有效期：1 天 */
    private static final long EXPIRATION_MILLIS = 1000 * 60 * 60 * 24;

    private JwtUtil() {
    }

    /** 根据用户生成 Token */
    public static String createToken(User user) {
        JwtBuilder jwtBuilder = Jwts.builder();
        return jwtBuilder
                .claim("username", user.getUsername())
                .setSubject(user.getUsername())
                .setIssuedAt(new java.util.Date())
                // Token 存活时间为 1 天
                .setExpiration(new java.util.Date(System.currentTimeMillis() + EXPIRATION_MILLIS))
                .setId(UUID.randomUUID().toString())
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }

    /** 校验 Token 是否有效（签名正确且未过期） */
    public static boolean checkToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        try {
            Jwts.parser().setSigningKey(SECRET).parseClaimsJws(token);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /** 解析 Token，获取其中的用户名 */
    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /** 解析 Token，异常时由调用方按业务处理 */
    public static Claims parseToken(String token) {
        Jws<Claims> claimsJws = Jwts.parser().setSigningKey(SECRET).parseClaimsJws(token);
        return claimsJws.getBody();
    }
}
