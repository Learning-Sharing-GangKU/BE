package com.gangku.be.service.auth;

import com.gangku.be.config.auth.EmailVerificationProps;
import com.gangku.be.constant.auth.TokenProperty;
import com.gangku.be.domain.User;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.AuthErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.model.TokenPair;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.AuthService;
import com.gangku.be.util.jwt.EmailVerificationJwt;
import com.gangku.be.util.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReIssueServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    // ---- 생성자 주입을 위해 필요하지만 reIssue()에서는 직접 안 쓰는 의존성들 ----
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private EmailVerificationJwt emailVerificationJwt;

    @Mock
    private EmailVerificationProps emailVerificationProps;

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private AuthService authService;

    // ============================================================
    // 1) 정상: 유효한 refresh_token 쿠키 → 토큰 회전
    // ============================================================
    @Test
    void reIssue_withValidRefreshTokenCookie_returnsRotatedTokenPair() {
        // given
        String oldRefreshToken = "old-refresh-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        Long userId = 1L;

        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = { new Cookie("refresh_token", oldRefreshToken) };
        when(request.getCookies()).thenReturn(cookies);

        when(jwtTokenProvider.isRefreshToken(oldRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.getSubject(oldRefreshToken)).thenReturn(String.valueOf(userId));

        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(userId);
        when(user.getRefreshToken()).thenReturn(oldRefreshToken);

        when(jwtTokenProvider.generateAccessToken(String.valueOf(userId)))
                .thenReturn(newAccessToken);
        when(jwtTokenProvider.generateRefreshToken(String.valueOf(userId)))
                .thenReturn(newRefreshToken);

        // when
        TokenPair result = authService.reIssue(request);

        // then
        assertNotNull(result);
        assertEquals(newAccessToken, result.accessToken());
        assertEquals(newRefreshToken, result.refreshToken());

        verify(jwtTokenProvider).isRefreshToken(oldRefreshToken);
        verify(jwtTokenProvider).getSubject(oldRefreshToken);
        verify(userRepository).findById(userId);

        verify(user).updateRefreshToken(eq(newRefreshToken), any(LocalDateTime.class));
        verify(userRepository).save(user);
    }

    // ============================================================
    // 2) 예외: refresh_token 쿠키가 없는 경우
    // ============================================================
    @Test
    void reIssue_whenRefreshTokenCookieMissing_throwsRefreshTokenNotFound() {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = { new Cookie("other", "value") };
        when(request.getCookies()).thenReturn(cookies);

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> authService.reIssue(request));

        // then
        assertEquals(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND, ex.getErrorCode());

        verifyNoInteractions(jwtTokenProvider);
        verifyNoInteractions(userRepository);
    }

    // ============================================================
    // 3) 예외: 토큰 타입/형식이 refresh token 이 아님
    // ============================================================
    @Test
    void reIssue_withInvalidRefreshTokenType_throwsInvalidRefreshToken() {
        // given
        String badToken = "bad-token";

        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = { new Cookie("refresh_token", badToken) };
        when(request.getCookies()).thenReturn(cookies);

        when(jwtTokenProvider.isRefreshToken(badToken)).thenReturn(false);

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> authService.reIssue(request));

        // then
        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());

        verify(jwtTokenProvider).isRefreshToken(badToken);
        verify(jwtTokenProvider, never()).getSubject(anyString());
        verifyNoInteractions(userRepository);
    }

    // ============================================================
    // 4) 예외: refresh token에서 얻은 userId로 유저를 찾지 못함
    // ============================================================
    @Test
    void reIssue_withUnknownUserFromRefreshToken_throwsUserNotFound() {
        // given
        String refreshToken = "valid-refresh-token";
        Long userId = 99L;

        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = { new Cookie("refresh_token", refreshToken) };
        when(request.getCookies()).thenReturn(cookies);

        when(jwtTokenProvider.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getSubject(refreshToken)).thenReturn(String.valueOf(userId));

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> authService.reIssue(request));

        // then
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        verify(jwtTokenProvider).isRefreshToken(refreshToken);
        verify(jwtTokenProvider).getSubject(refreshToken);
        verify(userRepository).findById(userId);

        verify(jwtTokenProvider, never()).generateAccessToken(anyString());
        verify(jwtTokenProvider, never()).generateRefreshToken(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // ============================================================
    // 5) 예외: DB에 저장된 refreshToken 이 null 인 경우
    // ============================================================
    @Test
    void reIssue_whenStoredRefreshTokenIsNull_throwsTokenMismatch() {
        // given
        String refreshToken = "valid-refresh-token";
        Long userId = 1L;

        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = { new Cookie("refresh_token", refreshToken) };
        when(request.getCookies()).thenReturn(cookies);

        when(jwtTokenProvider.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getSubject(refreshToken)).thenReturn(String.valueOf(userId));

        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getRefreshToken()).thenReturn(null);

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> authService.reIssue(request));

        // then
        assertEquals(AuthErrorCode.TOKEN_MISMATCH, ex.getErrorCode());

        verify(user).getRefreshToken();
        verify(jwtTokenProvider, never()).generateAccessToken(anyString());
        verify(jwtTokenProvider, never()).generateRefreshToken(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // ============================================================
    // 6) 예외: DB에 저장된 refreshToken 과 쿠키의 값이 다른 경우
    // ============================================================
    @Test
    void reIssue_whenStoredRefreshTokenDiffers_throwsTokenMismatch() {
        // given
        String cookieToken = "cookie-token";
        String dbToken = "db-token";
        Long userId = 1L;

        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = { new Cookie("refresh_token", cookieToken) };
        when(request.getCookies()).thenReturn(cookies);

        when(jwtTokenProvider.isRefreshToken(cookieToken)).thenReturn(true);
        when(jwtTokenProvider.getSubject(cookieToken)).thenReturn(String.valueOf(userId));

        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getRefreshToken()).thenReturn(dbToken);

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> authService.reIssue(request));

        // then
        assertEquals(AuthErrorCode.TOKEN_MISMATCH, ex.getErrorCode());

        verify(jwtTokenProvider, never()).generateAccessToken(anyString());
        verify(jwtTokenProvider, never()).generateRefreshToken(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    // ============================================================
    // 7) 경계: 여러 쿠키 중에서 refresh_token 쿠키를 정확히 사용
    // ============================================================
    @Test
    void reIssue_withMultipleCookies_usesRefreshTokenCookieCorrectly() {
        // given
        String oldRefreshToken = "old-refresh-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        Long userId = 1L;

        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = {
                new Cookie("JSESSIONID", "abc"),
                new Cookie("refresh_token", oldRefreshToken),
                new Cookie("theme", "dark")
        };
        when(request.getCookies()).thenReturn(cookies);

        when(jwtTokenProvider.isRefreshToken(oldRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.getSubject(oldRefreshToken)).thenReturn(String.valueOf(userId));

        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(userId);
        when(user.getRefreshToken()).thenReturn(oldRefreshToken);

        when(jwtTokenProvider.generateAccessToken(String.valueOf(userId)))
                .thenReturn(newAccessToken);
        when(jwtTokenProvider.generateRefreshToken(String.valueOf(userId)))
                .thenReturn(newRefreshToken);

        // when
        TokenPair result = authService.reIssue(request);

        // then
        assertNotNull(result);
        assertEquals(newAccessToken, result.accessToken());
        assertEquals(newRefreshToken, result.refreshToken());

        verify(jwtTokenProvider).isRefreshToken(oldRefreshToken);
        verify(jwtTokenProvider).getSubject(oldRefreshToken);
        verify(userRepository).findById(userId);
        verify(user).updateRefreshToken(eq(newRefreshToken), any(LocalDateTime.class));
        verify(userRepository).save(user);
    }

    // ============================================================
    // 8) 경계(시간): refreshExpiry가 설정된 만료일 범위 내인지 검증
    // ============================================================
    @Test
    void reIssue_refreshExpirySetWithConfiguredDays() {
        // given
        String oldRefreshToken = "old-refresh-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        Long userId = 1L;

        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = { new Cookie("refresh_token", oldRefreshToken) };
        when(request.getCookies()).thenReturn(cookies);

        when(jwtTokenProvider.isRefreshToken(oldRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.getSubject(oldRefreshToken)).thenReturn(String.valueOf(userId));

        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(userId);
        when(user.getRefreshToken()).thenReturn(oldRefreshToken);

        when(jwtTokenProvider.generateAccessToken(String.valueOf(userId)))
                .thenReturn(newAccessToken);
        when(jwtTokenProvider.generateRefreshToken(String.valueOf(userId)))
                .thenReturn(newRefreshToken);

        long expirationDays = TokenProperty.REFRESH_TOKEN.getExpirationInDays();

        // when
        LocalDateTime before = LocalDateTime.now();
        authService.reIssue(request);
        LocalDateTime after = LocalDateTime.now();

        // then
        ArgumentCaptor<LocalDateTime> expiryCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(user).updateRefreshToken(eq(newRefreshToken), expiryCaptor.capture());
        LocalDateTime expiry = expiryCaptor.getValue();

        assertNotNull(expiry);

        // expiry 는 updateRefreshToken 내부에서 now.plusDays(expirationDays) 로 설정됨
        LocalDateTime lowerBound = before.plusDays(expirationDays);
        LocalDateTime upperBound = after.plusDays(expirationDays);

        assertTrue(!expiry.isBefore(lowerBound),
                () -> "expiry should be on/after lowerBound: " + expiry + " vs " + lowerBound);
        assertTrue(!expiry.isAfter(upperBound),
                () -> "expiry should be on/before upperBound: " + expiry + " vs " + upperBound);

        verify(userRepository).save(user);
    }
}
