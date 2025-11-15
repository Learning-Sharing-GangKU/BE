package com.gangku.be.service;

import com.gangku.be.constant.auth.TokenProperty;
import com.gangku.be.domain.User;
import com.gangku.be.dto.user.LoginRequestDto;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.security.jwt.JwtTokenProvider;
import com.gangku.be.security.jwt.TokenPair;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * 기능 목록
     * 1. 로그인
     * 2. 토큰 재발급
     * 3. 로그아웃
     */

    public TokenPair login(LoginRequestDto loginRequestDto) {

        // 1) 이메일 / 비밀번호 검증 및 유저 반환
        User user = findUserByEmailAndPassword(loginRequestDto.getEmail(), loginRequestDto.getPassword());

        // 2) Token 생성
        TokenPair tokenPair = generateToken(user.getId());

        // 3) Refresh Token DB에 갱신
        updateRefreshToken(user, tokenPair.refreshToken(), TokenProperty.REFRESH_TOKEN.getExpirationInDays());

        return tokenPair;
    }

    public TokenPair reIssue(HttpServletRequest request) {

        // 1) 쿠키에서 Refresh Token 꺼내기
        String refreshToken = findRefreshTokenFromCookie(request);

        // 2) Refresh Token 기본 검증
        validateRefreshToken(refreshToken);

        // 3) Refresh Token으로부터 User 정보 가져오기
        User user = findUserFromRefreshToken(refreshToken);

        // 5) DB의 Refresh Token와 검증
        verifyRefreshToken(user, refreshToken);

        // 6) 토큰 회전 - 새 Access + 새 Refresh Token 발급
        TokenPair tokenPair = generateToken(user.getId());

        // 7) DB에 발급한 새 Refresh Token 저장
        updateRefreshToken(user, refreshToken, TokenProperty.REFRESH_TOKEN.getExpirationInDays());

        return tokenPair;
    }

    public void logout(HttpServletRequest request) {

        // 1) 쿠키에서 Refresh Token 꺼내기
        String refreshToken = findRefreshTokenFromCookie(request);

        // 2) 토큰에서 userId 추출 및 DB에서 유저 조회
        User user = findUserFromRefreshToken(refreshToken);

        // 3) Refresh Token 기본 검증
        verifyRefreshToken(user, refreshToken);

        // 3. DB에 저장된 리프레시 토큰 제거
        updateRefreshToken(user, refreshToken, 0L);
    }

    /**
     * --- 비즈니스 로직 헬퍼 메서드 ---
     */

    private TokenPair generateToken(Long userId) {

        String accessToken = jwtTokenProvider.generateAccessToken(String.valueOf(userId));
        String refreshToken = jwtTokenProvider.generateRefreshToken(String.valueOf(userId));

        return new TokenPair(accessToken, refreshToken);
    }

    private void updateRefreshToken(User user, String refreshToken, Long plusDays) {
        user.updateRefreshToken(refreshToken, LocalDateTime.now().plusDays(plusDays));
        userRepository.save(user);
    }

    /**
     * --- 검증 및 반환 헬퍼 메서드 ---
     */

    private User findUserByEmailAndPassword(String email, String rawPassword) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return user;
    }

    private User findUserFromRefreshToken(String refreshToken) {

        String userId = jwtTokenProvider.getSubject(refreshToken);

        return userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private String findRefreshTokenFromCookie(HttpServletRequest request) {

        return Arrays.stream(request.getCookies())
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("리프레시 토큰이 없습니다."));
    }

    private void validateRefreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) ||
                !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }
    }

    private void verifyRefreshToken(User user, String refreshToken) {
        if (user.getRefreshToken() == null ||
                !user.getRefreshToken().equals(refreshToken)) {
            throw new IllegalArgumentException("서버에 저장된 리프레시 토큰과 일치하지 않습니다.");
        }
    }
}
