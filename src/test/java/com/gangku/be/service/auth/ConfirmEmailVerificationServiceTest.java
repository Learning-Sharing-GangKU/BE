package com.gangku.be.service.auth;

import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.AuthErrorCode;
import com.gangku.be.model.auth.EmailVerificationConfirmResult;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.AuthService;
import com.gangku.be.util.jwt.EmailVerificationJwt;
import com.gangku.be.util.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gangku.be.config.auth.EmailVerificationProps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.confirmEmailVerification()
 */
@ExtendWith(MockitoExtension.class)
class ConfirmEmailVerificationServiceTest {

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
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private static String sessionKey(String sessionId) {
        return "auth:signup:session:" + sessionId;
    }

    private static String verifiedEmailKey(String email) {
        return "auth:signup:verified-email:" + email;
    }

    @Test
    @DisplayName("정상: 유효한 세션과 인증된 이메일이면 성공 결과 반환")
    void confirmEmailVerification_withValidSessionAndVerifiedEmail_returnsSuccessResult() {
        // given
        String sessionId = "session-123";
        String sessionKey = sessionKey(sessionId);
        String email = "user@konkuk.ac.kr";
        String verifiedKey = verifiedEmailKey(email);

        when(stringRedisTemplate.hasKey(sessionKey)).thenReturn(true);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(sessionKey, "email")).thenReturn(email);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(verifiedKey)).thenReturn("1");

        // when
        EmailVerificationConfirmResult result = authService.confirmEmailVerification(sessionId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.verified()).isTrue();
        assertThat(result.email()).isEqualTo(email);

        verify(hashOperations).put(sessionKey, "verified", "1");
        verify(stringRedisTemplate).delete(verifiedKey);
    }

    @Test
    @DisplayName("정상: verifiedEmailFlag가 1이 아니어도 non-null이면 인증 완료로 처리")
    void confirmEmailVerification_withNonOneVerifiedFlag_stillTreatsAsVerified() {
        // given
        String sessionId = "session-456";
        String sessionKey = sessionKey(sessionId);
        String email = "user2@konkuk.ac.kr";
        String verifiedKey = verifiedEmailKey(email);

        when(stringRedisTemplate.hasKey(sessionKey)).thenReturn(true);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(sessionKey, "email")).thenReturn(email);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(verifiedKey)).thenReturn("Y"); // non-null, non-"1"

        // when
        EmailVerificationConfirmResult result = authService.confirmEmailVerification(sessionId);

        // then
        assertThat(result.verified()).isTrue();
        assertThat(result.email()).isEqualTo(email);
    }

    @Test
    @DisplayName("정상: 세션 verified=1 마킹이 이메일 플래그 삭제보다 먼저 호출됨")
    void confirmEmailVerification_callsMarkSessionVerifiedBeforeConsumeFlag() {
        // given
        String sessionId = "order-check-session";
        String sessionKey = sessionKey(sessionId);
        String email = "order@konkuk.ac.kr";
        String verifiedKey = verifiedEmailKey(email);

        when(stringRedisTemplate.hasKey(sessionKey)).thenReturn(true);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(sessionKey, "email")).thenReturn(email);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(verifiedKey)).thenReturn("1");

        // when
        authService.confirmEmailVerification(sessionId);

        // then
        InOrder inOrder = inOrder(hashOperations, stringRedisTemplate);

        // markSessionAsVerified(sessionKey)
        inOrder.verify(hashOperations).put(sessionKey, "verified", "1");
        // consumeVerifiedEmailFlag(email)
        inOrder.verify(stringRedisTemplate).delete(verifiedKey);
    }

    @Test
    @DisplayName("예외: sessionId가 null이면 INVALID_SESSION 예외")
    void confirmEmailVerification_withNullSessionId_throwsInvalidSession() {
        // when
        CustomException ex = assertThrows(
                CustomException.class,
                () -> authService.confirmEmailVerification(null)
        );

        // then
        assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_SESSION);
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("예외: sessionId가 공백이면 INVALID_SESSION 예외")
    void confirmEmailVerification_withBlankSessionId_throwsInvalidSession() {
        // when
        CustomException ex = assertThrows(
                CustomException.class,
                () -> authService.confirmEmailVerification("   ")
        );

        // then
        assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_SESSION);
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("예외: 세션 키가 Redis에 존재하지 않으면 INVALID_SESSION 예외")
    void confirmEmailVerification_withNonExistingSessionKey_throwsInvalidSession() {
        // given
        String sessionId = "missing-session";
        String sessionKey = sessionKey(sessionId);

        when(stringRedisTemplate.hasKey(sessionKey)).thenReturn(false);

        // when
        CustomException ex = assertThrows(
                CustomException.class,
                () -> authService.confirmEmailVerification(sessionId)
        );

        // then
        assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_SESSION);
        verify(stringRedisTemplate).hasKey(sessionKey);
        verifyNoMoreInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("예외: hasKey가 null을 반환하면 INVALID_SESSION 예외")
    void confirmEmailVerification_whenHasKeyReturnsNull_throwsInvalidSession() {
        // given
        String sessionId = "weird-session";
        String sessionKey = sessionKey(sessionId);

        when(stringRedisTemplate.hasKey(sessionKey)).thenReturn(null);

        // when
        CustomException ex = assertThrows(
                CustomException.class,
                () -> authService.confirmEmailVerification(sessionId)
        );

        // then
        assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_SESSION);
        verify(stringRedisTemplate).hasKey(sessionKey);
        verifyNoMoreInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("예외: 세션 Hash에 email 필드가 없으면 INVALID_SESSION 예외")
    void confirmEmailVerification_withSessionMissingEmailField_throwsInvalidSession() {
        // given
        String sessionId = "no-email-session";
        String sessionKey = sessionKey(sessionId);

        when(stringRedisTemplate.hasKey(sessionKey)).thenReturn(true);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(sessionKey, "email")).thenReturn(null);

        // when
        CustomException ex = assertThrows(
                CustomException.class,
                () -> authService.confirmEmailVerification(sessionId)
        );

        // then
        assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_SESSION);
        verify(hashOperations, never()).put(eq(sessionKey), eq("verified"), anyString());
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("예외: 이메일 인증 플래그가 없으면 VERIFICATION_NOT_STARTED 예외")
    void confirmEmailVerification_withUnverifiedEmail_throwsVerificationNotStarted() {
        // given
        String sessionId = "unverified-session";
        String sessionKey = sessionKey(sessionId);
        String email = "not-verified@konkuk.ac.kr";
        String verifiedKey = verifiedEmailKey(email);

        when(stringRedisTemplate.hasKey(sessionKey)).thenReturn(true);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(sessionKey, "email")).thenReturn(email);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(verifiedKey)).thenReturn(null);

        // when
        CustomException ex = assertThrows(
                CustomException.class,
                () -> authService.confirmEmailVerification(sessionId)
        );

        // then
        assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.VERIFICATION_NOT_STARTED);
        verify(hashOperations, never()).put(eq(sessionKey), eq("verified"), anyString());
        verify(stringRedisTemplate, never()).delete(verifiedKey);
    }

    @Test
    @DisplayName("경계: 매우 긴 세션 ID도 정상 처리")
    void confirmEmailVerification_withVeryLongSessionId_stillResolvesKeyCorrectly() {
        // given
        String longSessionId = "session-" + "x".repeat(256);
        String sessionKey = sessionKey(longSessionId);
        String email = "long-session@konkuk.ac.kr";
        String verifiedKey = verifiedEmailKey(email);

        when(stringRedisTemplate.hasKey(sessionKey)).thenReturn(true);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(sessionKey, "email")).thenReturn(email);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(verifiedKey)).thenReturn("1");

        // when
        EmailVerificationConfirmResult result = authService.confirmEmailVerification(longSessionId);

        // then
        assertThat(result.verified()).isTrue();
        assertThat(result.email()).isEqualTo(email);
    }

    @Test
    @DisplayName("경계: delete가 0을 반환해도 예외 없이 성공 처리")
    void confirmEmailVerification_whenDeleteFlagFails_doesNotThrow() {
        // given
        String sessionId = "delete-fail-session";
        String sessionKey = sessionKey(sessionId);
        String email = "delete-fail@konkuk.ac.kr";
        String verifiedKey = verifiedEmailKey(email);

        when(stringRedisTemplate.hasKey(sessionKey)).thenReturn(true);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(sessionKey, "email")).thenReturn(email);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(verifiedKey)).thenReturn("1");
        when(stringRedisTemplate.delete(verifiedKey)).thenReturn(false); // 삭제 실패 시나리오

        // when
        EmailVerificationConfirmResult result = authService.confirmEmailVerification(sessionId);

        // then
        assertThat(result.verified()).isTrue();
        assertThat(result.email()).isEqualTo(email);
        verify(stringRedisTemplate).delete(verifiedKey);
    }
}
