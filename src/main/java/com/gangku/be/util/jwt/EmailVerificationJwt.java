package com.gangku.be.util.jwt;

import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.AuthErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class EmailVerificationJwt {

    private final Key signingKey;

    public record EmailVerificationToken(String token, String tokenId, Instant expiresAt) {}

    public EmailVerificationJwt(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public EmailVerificationToken generateToken(String email, Duration timeToLive) {
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

    public Jws<Claims> parseClaims(String jwt) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(jwt);
        } catch (ExpiredJwtException e) {
            throw new CustomException(AuthErrorCode.EMAIL_TOKEN_EXPIRED_OR_USED);
        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            throw new CustomException(AuthErrorCode.INVALID_EMAIL_VERIFICATION_TOKEN);
        }
    }
}
