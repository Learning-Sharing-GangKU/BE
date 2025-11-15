package com.gangku.be.security.jwt;

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

    // 별도 시크릿이 없으면 기존 jwt.secret을 그대로 재사용 (변경 최소화)
    private final Key key;
    private final String issuer;

    public EmailVerificationJwt(
            @Value("${app.email.verification.secret:}") String evSecretOpt,
            @Value("${jwt.secret}") String fallback,
            @Value("${app.email.verification.issuer:gangku-api}") String issuer) {
        String secret = (evSecretOpt == null || evSecretOpt.isBlank()) ? fallback : evSecretOpt;
        // 기존 JwtTokenProvider가 Base64 키를 쓰고 있어 동일하게 처리
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception e) {
            // 혹시 평문 키가 들어오면 그대로 사용 (dev 편의)
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
    }

    public record EvJwt(String token, String jti, Instant exp) {}

    public EvJwt create(String email, Duration ttl) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant exp = now.plus(ttl);
        String token = Jwts.builder()
                .setHeaderParam("typ", "EVJWT")
                .setIssuer(issuer)
                .setAudience("signup")
                .setSubject(email)
                .setId(jti)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        return new EvJwt(token, jti, exp);
    }

    public Jws<Claims> parse(String jwt) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt);
    }
}
