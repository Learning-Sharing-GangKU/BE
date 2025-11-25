package com.gangku.be.service.auth;

import com.gangku.be.config.auth.EmailVerificationProps;
import com.gangku.be.domain.User;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.AuthErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.AuthService;
import com.gangku.be.util.jwt.EmailVerificationJwt;
import com.gangku.be.util.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    // --- 이메일 인증 관련 의존성 (logout에서는 직접 안 쓰이지만 생성자 주입을 위해 필요) ---
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
    // 1) 정상: 유효한 refresh_token 쿠키 → DB의 refreshToken 제거
    // ============================================================
    @Test
    void logout_withValidRefreshTokenCookie_clearsStoredToken() {
        // given
        String refreshToken = "valid-refresh-token";
        Long userId = 1L;

        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = { new Cookie("refresh_token", refreshToken) };
        when(request.getCookies()).thenReturn(cookies);

        when(jwtTokenProvider.getSubject(refreshToken)).thenReturn(String.valueOf(userId));

        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getRefreshToken()).thenReturn(refreshToken);

        // when
        authService.logout(request);

        // then
        verify(jwtTokenProvider).getSubject(refreshToken);
        verify(userRepository).findById(userId);

        // 로그아웃 시점에는 refreshToken 제거 로직이 수행되어야 함
        verify(user).clearRefreshToken();
        verify(userRepository).save(user);
    }

    // ============================================================
    // 2) 예외: refresh_token 쿠키가 없는 경우
    // ============================================================
    @Test
    void logout_whenRefreshTokenCookieMissing_throwsRefreshTokenNotFound() {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = { new Cookie("JSESSIONID", "abc") }; // refresh_token 없음
        when(request.getCookies()).thenReturn(cookies);

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> authService.logout(request));

        // then
        assertEquals(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND, ex.getErrorCode());

        verifyNoInteractions(jwtTokenProvider);
        verifyNoInteractions(userRepository);
    }

    // ============================================================
    // 3) 예외: refresh token에서 얻은 userId로 유저를 찾지 못함
    // ============================================================
    @Test
    void logout_withUnknownUserFromRefreshToken_throwsUserNotFound() {
        // given
        String refreshToken = "valid-refresh-token";
        Long userId = 99L;

        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = { new Cookie("refresh_token", refreshToken) };
        when(request.getCookies()).thenReturn(cookies);

        when(jwtTokenProvider.getSubject(refreshToken)).thenReturn(String.valueOf(userId));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> authService.logout(request));

        // then
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        verify(jwtTokenProvider).getSubject(refreshToken);
        verify(userRepository).findById(userId);

        verify(userRepository, never()).save(any(User.class));
    }

    // ============================================================
    // 4) 예외: DB에 저장된 refreshToken 이 null 인 경우
    // ============================================================
    @Test
    void logout_whenStoredRefreshTokenIsNull_throwsTokenMismatch() {
        // given
        String refreshToken = "valid-refresh-token";
        Long userId = 1L;

        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = { new Cookie("refresh_token", refreshToken) };
        when(request.getCookies()).thenReturn(cookies);

        when(jwtTokenProvider.getSubject(refreshToken)).thenReturn(String.valueOf(userId));

        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getRefreshToken()).thenReturn(null);

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> authService.logout(request));

        // then
        assertEquals(AuthErrorCode.TOKEN_MISMATCH, ex.getErrorCode());

        verify(jwtTokenProvider).getSubject(refreshToken);
        verify(userRepository).findById(userId);

        // 토큰 불일치이므로 실제 삭제/저장은 수행되지 않아야 함
        verify(user, never()).clearRefreshToken();
        verify(userRepository, never()).save(user);
    }

    // ============================================================
    // 5) 예외: DB에 저장된 refreshToken 과 쿠키의 값이 다른 경우
    // ============================================================
    @Test
    void logout_whenStoredRefreshTokenDiffers_throwsTokenMismatch() {
        // given
        String cookieToken = "cookie-token";
        String dbToken = "db-token";
        Long userId = 1L;

        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = { new Cookie("refresh_token", cookieToken) };
        when(request.getCookies()).thenReturn(cookies);

        when(jwtTokenProvider.getSubject(cookieToken)).thenReturn(String.valueOf(userId));

        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getRefreshToken()).thenReturn(dbToken);

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> authService.logout(request));

        // then
        assertEquals(AuthErrorCode.TOKEN_MISMATCH, ex.getErrorCode());

        verify(jwtTokenProvider).getSubject(cookieToken);
        verify(userRepository).findById(userId);

        verify(user, never()).clearRefreshToken();
        verify(userRepository, never()).save(user);
    }

    // ============================================================
    // 6) 경계: 여러 쿠키 중에서 refresh_token 쿠키를 정확히 사용
    // ============================================================
    @Test
    void logout_withMultipleCookies_usesRefreshTokenCookieCorrectly() {
        // given
        String refreshToken = "valid-refresh-token";
        Long userId = 1L;

        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = {
                new Cookie("JSESSIONID", "abc"),
                new Cookie("refresh_token", refreshToken),
                new Cookie("theme", "dark")
        };
        when(request.getCookies()).thenReturn(cookies);

        when(jwtTokenProvider.getSubject(refreshToken)).thenReturn(String.valueOf(userId));

        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getRefreshToken()).thenReturn(refreshToken);

        // when
        authService.logout(request);

        // then
        verify(jwtTokenProvider).getSubject(refreshToken);
        verify(userRepository).findById(userId);

        verify(user).clearRefreshToken();
        verify(userRepository).save(user);
    }

    // ============================================================
    // 7) 경계: 이미 로그아웃된 사용자가 오래된 토큰으로 다시 로그아웃 시도
    //       (DB의 refreshToken 이 이미 null 인 상태)
    // ============================================================
    @Test
    void logout_whenUserAlreadyLoggedOutStoredTokenAlreadyCleared_throwsTokenMismatch() {
        // given
        String refreshToken = "stale-refresh-token";
        Long userId = 1L;

        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = { new Cookie("refresh_token", refreshToken) };
        when(request.getCookies()).thenReturn(cookies);

        when(jwtTokenProvider.getSubject(refreshToken)).thenReturn(String.valueOf(userId));

        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        // 이미 로그아웃되어 refreshToken 이 제거된 상태라고 가정
        when(user.getRefreshToken()).thenReturn(null);

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> authService.logout(request));

        // then
        assertEquals(AuthErrorCode.TOKEN_MISMATCH, ex.getErrorCode());

        verify(jwtTokenProvider).getSubject(refreshToken);
        verify(userRepository).findById(userId);

        verify(user, never()).clearRefreshToken();
        verify(userRepository, never()).save(user);
    }
}
