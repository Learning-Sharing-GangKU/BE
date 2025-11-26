package com.gangku.be.service.auth;

import com.gangku.be.config.auth.EmailVerificationProps;
import com.gangku.be.domain.User;
import com.gangku.be.dto.auth.LoginRequestDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.AuthErrorCode;
import com.gangku.be.model.TokenPair;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.AuthService;
import com.gangku.be.util.jwt.EmailVerificationJwt;
import com.gangku.be.util.jwt.JwtTokenProvider;
import com.gangku.be.constant.auth.TokenProperty;
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
class LoginServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    // --- 로그인에서 직접 쓰진 않지만, 생성자 주입을 위해 필요 ---
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
    // 1) 정상: 올바른 자격 증명 → TokenPair 반환
    // ============================================================
    @Test
    void login_withValidCredentials_returnsTokenPair() {
        // given
        String email = "test@konkuk.ac.kr";
        String rawPassword = "plain-password";
        String encodedPassword = "encoded-password";
        Long userId = 1L;

        LoginRequestDto request = new LoginRequestDto(email, rawPassword);

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getPassword()).thenReturn(encodedPassword);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        when(jwtTokenProvider.generateAccessToken(String.valueOf(userId)))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(String.valueOf(userId)))
                .thenReturn("refresh-token");

        // when
        TokenPair result = authService.login(request);

        // then
        assertNotNull(result);
        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());

        // refreshToken 갱신 후 save 호출
        verify(user).updateRefreshToken(eq("refresh-token"), any(LocalDateTime.class));
        verify(userRepository).save(user);
    }

    // ============================================================
    // 2) 경계: 기존 refreshToken이 있어도 새 토큰으로 덮어쓰기
    // ============================================================
    @Test
    void login_whenUserAlreadyHasRefreshToken_overwritesOldToken() {
        // given
        String email = "test@konkuk.ac.kr";
        String rawPassword = "plain-password";
        String encodedPassword = "encoded-password";
        Long userId = 1L;

        LoginRequestDto request = new LoginRequestDto(email, rawPassword);

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getPassword()).thenReturn(encodedPassword);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        when(jwtTokenProvider.generateAccessToken(String.valueOf(userId)))
                .thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(String.valueOf(userId)))
                .thenReturn("new-refresh-token");

        // when
        TokenPair result = authService.login(request);

        // then
        assertNotNull(result);
        assertEquals("new-access-token", result.accessToken());
        assertEquals("new-refresh-token", result.refreshToken());

        verify(user).updateRefreshToken(eq("new-refresh-token"), any(LocalDateTime.class));
        verify(userRepository).save(user);
    }

    // ============================================================
    // 3) 예외: 존재하지 않는 이메일
    // ============================================================
    @Test
    void login_withUnknownEmail_throwsInvalidCredential() {
        // given
        String email = "unknown@konkuk.ac.kr";
        String rawPassword = "plain-password";

        LoginRequestDto request = new LoginRequestDto(email, rawPassword);

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> authService.login(request));

        // then
        assertEquals(AuthErrorCode.INVALID_CREDENTIAL, ex.getErrorCode());

        verify(userRepository, never()).save(any(User.class));
        verify(jwtTokenProvider, never()).generateAccessToken(anyString());
        verify(jwtTokenProvider, never()).generateRefreshToken(anyString());
    }

    // ============================================================
    // 4) 예외: 비밀번호 불일치
    // ============================================================
    @Test
    void login_withWrongPassword_throwsInvalidCredential() {
        // given
        String email = "test@konkuk.ac.kr";
        String rawPassword = "wrong-password";
        String encodedPassword = "encoded-password";

        LoginRequestDto request = new LoginRequestDto(email, rawPassword);

        User user = mock(User.class);
        when(user.getPassword()).thenReturn(encodedPassword);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> authService.login(request));

        // then
        assertEquals(AuthErrorCode.INVALID_CREDENTIAL, ex.getErrorCode());

        verify(userRepository, never()).save(any(User.class));
        verify(jwtTokenProvider, never()).generateAccessToken(anyString());
        verify(jwtTokenProvider, never()).generateRefreshToken(anyString());
    }

    // ============================================================
    // 5) 경계: password == null 인 경우도 INVALID_CREDENTIAL로 처리
    // ============================================================
    @Test
    void login_withNullPassword_treatedAsInvalidCredential() {
        // given
        String email = "test@konkuk.ac.kr";
        String rawPassword = null;
        String encodedPassword = "encoded-password";

        LoginRequestDto request = new LoginRequestDto(email, rawPassword);

        User user = mock(User.class);
        when(user.getPassword()).thenReturn(encodedPassword);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        // passwordEncoder가 null을 받으면 false를 반환한다고 가정하고 스텁
        when(passwordEncoder.matches(null, encodedPassword)).thenReturn(false);

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> authService.login(request));

        // then
        assertEquals(AuthErrorCode.INVALID_CREDENTIAL, ex.getErrorCode());

        verify(userRepository, never()).save(any(User.class));
        verify(jwtTokenProvider, never()).generateAccessToken(anyString());
        verify(jwtTokenProvider, never()).generateRefreshToken(anyString());
    }

    // ============================================================
    // 6) 경계: 공백 이메일인 경우 → 존재하지 않는 이메일과 동일하게 INVALID_CREDENTIAL
    // ============================================================
    @Test
    void login_withBlankEmail_treatedAsInvalidCredential() {
        // given
        String email = " ";
        String rawPassword = "plain-password";

        LoginRequestDto request = new LoginRequestDto(email, rawPassword);

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> authService.login(request));

        // then
        assertEquals(AuthErrorCode.INVALID_CREDENTIAL, ex.getErrorCode());

        verify(userRepository, never()).save(any(User.class));
        verify(jwtTokenProvider, never()).generateAccessToken(anyString());
        verify(jwtTokenProvider, never()).generateRefreshToken(anyString());
    }

    // ============================================================
    // 7) 경계(시간): refreshExpiry가 설정된 만료일 범위 내인지 검증
    // ============================================================
    @Test
    void login_refreshExpirySetWithConfiguredDays() {
        // given
        String email = "test@konkuk.ac.kr";
        String rawPassword = "plain-password";
        String encodedPassword = "encoded-password";
        Long userId = 1L;

        LoginRequestDto request = new LoginRequestDto(email, rawPassword);

        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getPassword()).thenReturn(encodedPassword);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        when(jwtTokenProvider.generateAccessToken(String.valueOf(userId)))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(String.valueOf(userId)))
                .thenReturn("refresh-token");

        long expirationDays = TokenProperty.REFRESH_TOKEN.getExpirationInDays();

        // when
        LocalDateTime nowBefore = LocalDateTime.now();
        authService.login(request);
        LocalDateTime nowAfter = LocalDateTime.now();

        // then - updateRefreshToken에 전달된 expiry 인자를 캡쳐해서 검증
        ArgumentCaptor<LocalDateTime> expiryCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(user).updateRefreshToken(eq("refresh-token"), expiryCaptor.capture());
        LocalDateTime expiry = expiryCaptor.getValue();

        assertNotNull(expiry);

        // expiry 는 nowBefore 이후여야 하고,
        assertTrue(expiry.isAfter(nowBefore.minusSeconds(1)),
                () -> "expiry should be after nowBefore: " + expiry + " vs " + nowBefore);

        // nowAfter + expirationDays + 약간의 여유 범위 이전이어야 한다(대략적인 범위 체크)
        LocalDateTime upperBound = nowAfter.plusDays(expirationDays + 1);
        assertTrue(expiry.isBefore(upperBound),
                () -> "expiry should be before upperBound: " + expiry + " vs " + upperBound);

        verify(userRepository).save(user);
    }
}
