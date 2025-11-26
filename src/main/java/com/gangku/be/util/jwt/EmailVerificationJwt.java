package com.gangku.be.util.jwt;

import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.AuthErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class EmailVerificationJwt {

    // 이메일 인증용 JWT 서명에 사용할 키
    private final Key signingKey;

    // 이메일 인증 토큰의 내용을 표현하는 record
    public record EmailVerificationToken(String token, String tokenId, Instant expiresAt) {}

    public EmailVerificationJwt(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 이메일(email)을 subject로 갖는 이메일 인증용 JWT 생성
     */
    public EmailVerificationToken createToken(String email, Duration timeToLive) {
        String tokenId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(timeToLive);

        String token = Jwts.builder()
                .setSubject(email)
                .setId(tokenId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        return new EmailVerificationToken(token, tokenId, expiresAt);
    }

    public Jws<Claims> parseToken(String jwt) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(jwt);
        } catch (ExpiredJwtException e) {
            throw new CustomException(AuthErrorCode.TOKEN_EXPIRED_OR_USED);
        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }
}
