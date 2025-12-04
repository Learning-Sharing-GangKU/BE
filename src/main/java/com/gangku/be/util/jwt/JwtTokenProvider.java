package com.gangku.be.util.jwt;

import com.gangku.be.constant.auth.TokenProperty;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.AuthErrorCode;
import io.jsonwebtoken.*;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key signingKey;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + TokenProperty.ACCESS_TOKEN.getExpirationInMillis()))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(new Date())
                .claim("type", "refresh")
                .setExpiration(new Date(System.currentTimeMillis() + TokenProperty.REFRESH_TOKEN.getExpirationInMillis()))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new CustomException(AuthErrorCode.TOKEN_EXPIRED_OR_USED);
        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (CustomException e) {
            return false;
        }
    }

    // === JwtAuthFilter에서 기대하는 API 추가 ===

    /**
     * 토큰이 유효하면 true, 유효하지 않으면 false 반환.
     * (내부적으로는 parseClaims를 호출해서 CustomException을 캐치)
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (CustomException e) {
            // parseClaims에서 이미 AuthErrorCode로 매핑된 예외를 던지므로
            // 여기서는 필터 쪽에서 false로만 처리하도록 숨김
            return false;
        }
    }

    /**
     * JwtAuthFilter에서 사용하는 userId 추출용 메서드.
     * 현재 구현에서는 subject를 userId로 사용하고 있으므로 그대로 반환.
     */
    public String getUserIdFromToken(String token) {
        return getSubject(token);
    }
}