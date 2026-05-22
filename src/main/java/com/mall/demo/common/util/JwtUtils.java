

package com.mall.demo.common.util;

import cn.hutool.jwt.JWT;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // 在 JwtUtils 类中修改
    public String generateToken(String id, String userType) {
        long expirationMillis = expiration * 1000;
        return Jwts.builder()
                .subject(id)
                .claim("userType", userType)
                // 存入类型：USER, MERCHANT, ADMIN
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(getSigningKey())
                .compact();
    }

    // 增加获取类型的方法
    public String getUserTypeFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("userType", String.class);
    }




    public String getIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            log.info("【JWT解析】解析出的subject: {}", subject);
            log.info("【JWT解析】解析出的userId: {}", subject);

            return subject;
        } catch (ExpiredJwtException e) {
            log.error("【JWT解析】Token已过期: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("【JWT解析】Token解析异常: {}", e.getMessage(), e);
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("【JWT验证】JWT已过期: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("【JWT验证】JWT无效: {}", e.getMessage());
        }
        return false;
    }




}
