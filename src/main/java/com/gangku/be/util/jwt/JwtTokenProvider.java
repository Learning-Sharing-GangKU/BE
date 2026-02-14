package com.gangku.be.util.jwt;

import com.gangku.be.constant.auth.TokenProperty;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.AuthErrorCode;
import io.jsonwebtoken.*;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key signingKey;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String userId) {
        return Jwts.builder()
                .setIssuer("gangku-api")
                .setSubject(userId)
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + TokenProperty.ACCESS_TOKEN.getExpirationInMillis()))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .setIssuer("gangku-api")
                .setSubject(userId)
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + TokenProperty.REFRESH_TOKEN.getExpirationInMillis()))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token, AuthErrorCode.INVALID_ACCESS_TOKEN);

        if (!"access".equals(claims.get("type"))) {
            throw new CustomException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        Long userId = Long.parseLong(claims.getSubject());

        return new UsernamePasswordAuthenticationToken(
                userId,
                null,
                Collections.emptyList()
        );
    }

    public Long extractUserIdFromRefreshToken(String refreshToken) {
        Claims claims = parseClaims(refreshToken, AuthErrorCode.INVALID_REFRESH_TOKEN);

        String type = claims.get("type").toString();
        if (!"refresh".equals(type)) {
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        return Long.parseLong(claims.getSubject());
    }

    private Claims parseClaims(String token, AuthErrorCode invalidTokenErrorCode) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        }catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(invalidTokenErrorCode);
        }
    }
}