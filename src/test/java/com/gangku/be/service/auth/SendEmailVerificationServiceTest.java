package com.gangku.be.service.auth;

import com.gangku.be.config.auth.EmailVerificationProps;
import com.gangku.be.constant.auth.EmailConstants;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.AuthErrorCode;
import com.gangku.be.model.auth.EmailVerificationSendResult;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.AuthService;
import com.gangku.be.util.jwt.EmailVerificationJwt;
import com.gangku.be.util.jwt.EmailVerificationJwt.EmailVerificationToken;
import com.gangku.be.util.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendEmailVerificationServiceTest {

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

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // @Value 필드 주입 대체
        ReflectionTestUtils.setField(authService, "baseUrl", "https://example.com");
        ReflectionTestUtils.setField(authService, "fromEmailAddress", "no-reply@example.com");
    }

    // ---------------------------------------------------------
    // Helper: Redis ops stubbing (정상 플로우에서만 호출)
    // ---------------------------------------------------------
    private void stubRedisOps() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    private EmailVerificationToken stubEmailVerificationToken(String token, String tokenId, Instant expiresAt) {
        EmailVerificationToken tokenMock = mock(EmailVerificationToken.class);
        when(tokenMock.token()).thenReturn(token);
        when(tokenMock.tokenId()).thenReturn(tokenId);
        when(tokenMock.expiresAt()).thenReturn(expiresAt);
        return tokenMock;
    }

    // =========================================================
    // 1) 정상: 새로운 @konkuk.ac.kr 이메일 → 세션 & 토큰 반환
    // =========================================================
    @Test
    void sendEmailVerification_withValidNewKonkukEmail_returnsSessionAndToken() {
        // given
        String email = "user@konkuk.ac.kr";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        stubRedisOps();

        EmailVerificationToken tokenMock =
                stubEmailVerificationToken("jwt-token", "jti-123", Instant.now().plusSeconds(900));
        when(emailVerificationJwt.createToken(eq(email), any(Duration.class)))
                .thenReturn(tokenMock);

        // when
        EmailVerificationSendResult result = authService.sendEmailVerification(email);

        // then
        assertNotNull(result);
        assertNotNull(result.sessionId());
        assertFalse(result.sessionId().isBlank());
        assertEquals("jwt-token", result.emailVerificationToken());
        // sessionTtlMinutes는 props mock 기본값(0)일 수 있으므로 여기선 값 자체는 검증하지 않음

        verify(userRepository).findByEmail(email);
        verify(emailVerificationJwt).createToken(eq(email), any(Duration.class));
    }

    // =========================================================
    // 2) 정상: Redis에 화이트리스트 & 세션 저장 검증
    // =========================================================
    @Test
    void sendEmailVerification_withValidNewKonkukEmail_storesWhitelistAndSessionInRedis() {
        // given
        String email = "user@konkuk.ac.kr";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        stubRedisOps();

        EmailVerificationToken tokenMock =
                stubEmailVerificationToken("jwt-token", "jti-123", Instant.now().plusSeconds(900));
        when(emailVerificationJwt.createToken(eq(email), any(Duration.class)))
                .thenReturn(tokenMock);

        // when
        authService.sendEmailVerification(email);

        // then
        // 1) JTI 화이트리스트 키 검증
        ArgumentCaptor<String> emailKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(valueOperations).set(emailKeyCaptor.capture(), eq(email), ttlCaptor.capture());

        String emailKey = emailKeyCaptor.getValue();
        assertTrue(emailKey.startsWith("auth:signup:email-verification-token:"),
                "emailVerificationTokenKey prefix mismatch");

        Duration ttl = ttlCaptor.getValue();
        assertNotNull(ttl);

        // 2) 세션 키 / 해시 필드 검증
        ArgumentCaptor<String> sessionKeyCaptor = ArgumentCaptor.forClass(String.class);

        verify(hashOperations).put(sessionKeyCaptor.capture(), eq("email"), eq(email));
        String sessionKey = sessionKeyCaptor.getValue();
        assertTrue(sessionKey.startsWith("auth:signup:session:"), "signupSessionKey prefix mismatch");

        verify(hashOperations).put(eq(sessionKey), eq("verified"), eq("0"));
        verify(stringRedisTemplate).expire(eq(sessionKey), any(Duration.class));
    }

    // =========================================================
    // 3) 정상: 인증 메일 발송 여부 검증
    // =========================================================
    @Test
    void sendEmailVerification_withValidNewKonkukEmail_sendsVerificationMail() {
        // given
        String email = "user@konkuk.ac.kr";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        stubRedisOps();

        EmailVerificationToken tokenMock =
                stubEmailVerificationToken("jwt-token", "jti-123", Instant.now().plusSeconds(900));
        when(emailVerificationJwt.createToken(eq(email), any(Duration.class)))
                .thenReturn(tokenMock);

        // when
        authService.sendEmailVerification(email);

        // then
        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(mailCaptor.capture());

        SimpleMailMessage mail = mailCaptor.getValue();
        assertNotNull(mail);

        assertArrayEquals(new String[]{email}, mail.getTo());
        assertEquals(EmailConstants.VERIFICATION_SUBJECT, mail.getSubject());

        String body = mail.getText();
        assertNotNull(body);
        assertTrue(body.contains("jwt-token"), "mail body should contain token");
        assertTrue(body.contains("https://example.com"), "mail body should contain baseUrl");
    }

    // =========================================================
    // 4) 예외: email == null → INVALID_EMAIL_FORMAT
    // =========================================================
    @Test
    void sendEmailVerification_withNullEmail_throwsInvalidEmailFormat() {
        // given
        String email = null;

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> authService.sendEmailVerification(email));

        // then
        assertEquals(AuthErrorCode.INVALID_EMAIL_FORMAT, ex.getErrorCode());

        verifyNoInteractions(userRepository);
        verifyNoInteractions(emailVerificationJwt);
        verifyNoInteractions(javaMailSender);
    }

    // =========================================================
    // 5) 예외: @konkuk.ac.kr 이 아닌 이메일 → INVALID_EMAIL_FORMAT
    // =========================================================
    @Test
    void sendEmailVerification_withNonKonkukEmail_throwsInvalidEmailFormat() {
        // given
        String email = "user@gmail.com";

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> authService.sendEmailVerification(email));

        // then
        assertEquals(AuthErrorCode.INVALID_EMAIL_FORMAT, ex.getErrorCode());

        verifyNoInteractions(userRepository);
        verifyNoInteractions(emailVerificationJwt);
        verifyNoInteractions(javaMailSender);
    }

    // =========================================================
    // 6) 예외: 이미 등록된 이메일 → EMAIL_CONFLICT
    // =========================================================
    @Test
    void sendEmailVerification_withAlreadyRegisteredEmail_throwsEmailConflict() {
        // given
        String email = "user@konkuk.ac.kr";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(mock(com.gangku.be.domain.User.class)));

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> authService.sendEmailVerification(email));

        // then
        assertEquals(AuthErrorCode.EMAIL_CONFLICT, ex.getErrorCode());

        verify(userRepository).findByEmail(email);
        verifyNoInteractions(emailVerificationJwt);
        verifyNoInteractions(javaMailSender);
    }

    // =========================================================
    // 7) 경계: 대문자 도메인 → 유효한 이메일로 처리
    // =========================================================
    @Test
    void sendEmailVerification_withUppercaseKonkukDomain_treatedAsValidEmail() {
        // given
        String email = "User@KONKUK.AC.KR";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        stubRedisOps();

        EmailVerificationToken tokenMock =
                stubEmailVerificationToken("jwt-token", "jti-123", Instant.now().plusSeconds(900));
        when(emailVerificationJwt.createToken(eq(email), any(Duration.class)))
                .thenReturn(tokenMock);

        // when
        EmailVerificationSendResult result = authService.sendEmailVerification(email);

        // then
        assertNotNull(result);
        assertEquals("jwt-token", result.emailVerificationToken());
        assertNotNull(result.sessionId());
        assertFalse(result.sessionId().isBlank());

        verify(userRepository).findByEmail(email);
        verify(emailVerificationJwt).createToken(eq(email), any(Duration.class));
    }

    // =========================================================
    // 8) 경계(시간): Redis TTL이 양수이며 기대 범위 내에 설정되는지
    // =========================================================
    @Test
    void sendEmailVerification_setsTokenAndSessionTtlWithinExpectedRange() {
        // given
        String email = "user@konkuk.ac.kr";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        stubRedisOps();

        // 만료 시각을 "지금 + 900초" 로 설정
        Instant expiresAt = Instant.now().plusSeconds(900);
        EmailVerificationToken tokenMock =
                stubEmailVerificationToken("jwt-token", "jti-123", expiresAt);
        when(emailVerificationJwt.createToken(eq(email), any(Duration.class)))
                .thenReturn(tokenMock);

        // when
        authService.sendEmailVerification(email);

        // then
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(anyString(), eq(email), ttlCaptor.capture());

        Duration ttl = ttlCaptor.getValue();
        assertNotNull(ttl);

        long seconds = ttl.getSeconds();
        assertTrue(seconds > 0, "TTL should be positive");
        assertTrue(seconds <= 900, "TTL should not exceed 900 seconds");
    }

    // =========================================================
    // 9) 경계(메일 본문): baseUrl + path + token 형태의 URL이 포함되는지
    // =========================================================
    @Test
    void sendEmailVerification_buildsVerificationUrlWithBaseUrlAndToken() {
        // given
        String email = "user@konkuk.ac.kr";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        stubRedisOps();

        EmailVerificationToken tokenMock =
                stubEmailVerificationToken("jwt-token", "jti-123", Instant.now().plusSeconds(900));
        when(emailVerificationJwt.createToken(eq(email), any(Duration.class)))
                .thenReturn(tokenMock);

        // when
        authService.sendEmailVerification(email);

        // then
        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(mailCaptor.capture());

        SimpleMailMessage mail = mailCaptor.getValue();
        assertNotNull(mail);

        String body = mail.getText();
        assertNotNull(body);

        String expectedUrlPrefix = "https://example.com" + EmailConstants.VERIFICATION_PATH;
        assertTrue(body.contains(expectedUrlPrefix),
                "mail body should contain verification URL prefix");
        assertTrue(body.contains("jwt-token"),
                "mail body should contain the token at the end of URL");
    }
}
