package com.example.myfirstspringboot.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 *
 * <p>
 * 提供 JWT Token 的生成、解析和验证功能
 * </p>
 */
@Slf4j
@Component
public class JwtUtil {

    /**
     * JWT 密钥，从配置文件读取，默认值为一个随机字符串
     * 生产环境应在配置文件中设置强密钥
     */
    @Value("${jwt.secret:mySecretKeyForJwtTokenGenerationAndValidation}")
    private String secret;

    /**
     * Token 有效期（毫秒），默认 7 天
     */
    @Value("${jwt.expiration:604800000}")
    private long expiration;

    /**
     * 生成 SecretKey
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     *
     * @param userId 用户 ID
     * @return JWT Token 字符串
     */
    public String generateToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从 Token 中提取用户 ID
     *
     * @param token JWT Token
     * @return 用户 ID
     */
    public String extractUserId(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 解析 Token 获取 Claims
     *
     * @param token JWT Token
     * @return Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token 已过期");
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的 JWT Token");
            return false;
        } catch (MalformedJwtException e) {
            log.warn("JWT Token 格式错误");
            return false;
        } catch (SecurityException e) {
            log.warn("JWT Token 签名验证失败");
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT Token 为空或非法");
            return false;
        }
    }

    /**
     * 检查 Token 是否即将过期
     *
     * @param token JWT Token
     * @param thresholdMillis 阈值（毫秒）
     * @return 是否即将过期
     */
    public boolean isTokenExpiredSoon(String token, long thresholdMillis) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            return expiration.getTime() - System.currentTimeMillis() < thresholdMillis;
        } catch (Exception e) {
            return true;
        }
    }
}
