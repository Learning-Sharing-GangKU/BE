package com.gangku.be.service.auth;

import com.gangku.be.config.auth.EmailVerificationProps;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.AuthErrorCode;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.AuthService;
import com.gangku.be.util.jwt.EmailVerificationJwt;
import com.gangku.be.util.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumeEmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private EmailVerificationJwt emailVerificationJwt;

    @Mock
    private EmailVerificationProps emailVerificationProps;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    // ----------------------------------------------------
    // Helper: JWT 파싱 결과 mock 생성
    // ----------------------------------------------------
    private Jws<Claims> stubParsedJwt(String tokenId) {
        @SuppressWarnings("unchecked")
        Jws<Claims> jwsMock = mock(Jws.class);
        Claims claimsMock = mock(Claims.class);

        when(jwsMock.getBody()).thenReturn(claimsMock);
        when(claimsMock.getId()).thenReturn(tokenId);

        return jwsMock;
    }

    // ====================================================
    // 1) 정상: 유효한 토큰 → 이메일 인증 플래그 세팅
    // ====================================================
    @Test
    void consumeEmailVerification_withValidToken_marksEmailAsVerified() {
        // given
        String tokenString = "valid-ev-token";
        String tokenId = "jti-123";
        String email = "user@konkuk.ac.kr";

        Jws<Claims> jwsMock = stubParsedJwt(tokenId);
        when(emailVerificationJwt.parseToken(tokenString)).thenReturn(jwsMock);

        // 화이트리스트 소비 → 이메일 반환
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList()))
                .thenReturn(email);

        // markEmailAsVerified() 에서 사용하는 부분
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        // TTL 값은 굳이 검증 안 할 케이스라, 기본값(0)이어도 상관없음

        // when
        authService.consumeEmailVerification(tokenString);

        // then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());

        String key = keyCaptor.getValue();
        String value = valueCaptor.getValue();
        Duration ttl = ttlCaptor.getValue();

        assertTrue(key.startsWith("auth:signup:verified-email:"), "verifiedEmailKey prefix mismatch");
        assertTrue(key.endsWith(email), "verifiedEmailKey should contain email");
        assertEquals("1", value);
        assertNotNull(ttl);
    }

    // ====================================================
    // 2) 정상: JTI 화이트리스트 키가 정확히 한 번만 소비되는지 검증
    // ====================================================
    @Test
    void consumeEmailVerification_withValidToken_consumesJtiExactlyOnce() {
        // given
        String tokenString = "valid-ev-token";
        String tokenId = "jti-123";
        String email = "user@konkuk.ac.kr";

        Jws<Claims> jwsMock = stubParsedJwt(tokenId);
        when(emailVerificationJwt.parseToken(tokenString)).thenReturn(jwsMock);

        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList()))
                .thenReturn(email);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        authService.consumeEmailVerification(tokenString);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);

        verify(stringRedisTemplate, times(1))
                .execute(any(DefaultRedisScript.class), keysCaptor.capture());

        List<String> keys = keysCaptor.getValue();
        assertNotNull(keys);
        assertEquals(1, keys.size());

        String key = keys.get(0);
        assertEquals("auth:signup:email-verification-token:" + tokenId, key);
    }

    // ====================================================
    // 3) 예외: Redis에서 이메일을 못 찾는 경우 → USER_NOT_FOUND_BY_TOKEN
    // ====================================================
    @Test
    void consumeEmailVerification_withUnknownTokenId_throwsUserNotFoundByToken() {
        // given
        String tokenString = "valid-but-consumed-token";
        String tokenId = "jti-unknown";

        Jws<Claims> jwsMock = stubParsedJwt(tokenId);
        when(emailVerificationJwt.parseToken(tokenString)).thenReturn(jwsMock);

        // 화이트리스트에서 이미 소비되었거나 존재하지 않음
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList()))
                .thenReturn(null);

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> authService.consumeEmailVerification(tokenString));

        // then
        assertEquals(AuthErrorCode.USER_NOT_FOUND_BY_TOKEN, ex.getErrorCode());

        // 이메일 인증 플래그는 설정되지 않아야 함
        verify(stringRedisTemplate, never()).opsForValue();
        verifyNoInteractions(valueOperations);
    }

    // ====================================================
    // 4) 예외: JWT 형식이 잘못된 경우 → JwtException
    // ====================================================
    @Test
    void consumeEmailVerification_withInvalidJwt_throwsTokenParseException() {
        // given
        String invalidToken = "invalid-jwt";

        when(emailVerificationJwt.parseToken(invalidToken))
                .thenThrow(new JwtException("invalid jwt"));

        // when & then
        assertThrows(JwtException.class,
                () -> authService.consumeEmailVerification(invalidToken));

        verifyNoInteractions(stringRedisTemplate);
    }

    // ====================================================
    // 5) 예외: 만료된 JWT인 경우 → JwtException (또는 ExpiredJwtException)
    // ====================================================
    @Test
    void consumeEmailVerification_withExpiredJwt_throwsTokenParseException() {
        // given
        String expiredToken = "expired-jwt";

        when(emailVerificationJwt.parseToken(expiredToken))
                .thenThrow(new JwtException("expired jwt"));

        // when & then
        assertThrows(JwtException.class,
                () -> authService.consumeEmailVerification(expiredToken));

        verifyNoInteractions(stringRedisTemplate);
    }

    // ====================================================
    // 6) 경계: null 토큰 문자열 → IllegalArgumentException (파서 수준에서 실패)
    // ====================================================
    @Test
    void consumeEmailVerification_withNullTokenString_throwsException() {
        // given
        when(emailVerificationJwt.parseToken(isNull()))
                .thenThrow(new IllegalArgumentException("token is null"));

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> authService.consumeEmailVerification(null));

        verifyNoInteractions(stringRedisTemplate);
    }

    // ====================================================
    // 7) 경계: 빈 문자열 토큰 → JwtException
    // ====================================================
    @Test
    void consumeEmailVerification_withEmptyTokenString_throwsException() {
        // given
        String emptyToken = "";

        when(emailVerificationJwt.parseToken(emptyToken))
                .thenThrow(new JwtException("empty token"));

        // when & then
        assertThrows(JwtException.class,
                () -> authService.consumeEmailVerification(emptyToken));

        verifyNoInteractions(stringRedisTemplate);
    }

    // ====================================================
    // 8) 경계(시간): verified 플래그 TTL이 설정값과 일치하는지 검증
    // ====================================================
    @Test
    void consumeEmailVerification_setsVerifiedFlagTtlWithinExpectedRange() {
        // given
        String tokenString = "valid-ev-token";
        String tokenId = "jti-123";
        String email = "user@konkuk.ac.kr";

        Jws<Claims> jwsMock = stubParsedJwt(tokenId);
        when(emailVerificationJwt.parseToken(tokenString)).thenReturn(jwsMock);

        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList()))
                .thenReturn(email);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // TTL 설정: 15분
        when(emailVerificationProps.getTokenTtlMinutes()).thenReturn(15L);

        // when
        authService.consumeEmailVerification(tokenString);

        // then
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(anyString(), eq("1"), ttlCaptor.capture());

        Duration ttl = ttlCaptor.getValue();
        assertNotNull(ttl);
        assertEquals(Duration.ofMinutes(15), ttl);
    }
}
