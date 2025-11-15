package com.gangku.be.controller;

import com.gangku.be.domain.User;
import com.gangku.be.dto.user.LoginRequestDto;
import com.gangku.be.dto.user.LoginResponseDto;
import com.gangku.be.security.jwt.JwtTokenProvider;
import com.gangku.be.security.jwt.TokenPair;
import com.gangku.be.service.AuthService;
import com.gangku.be.service.UserService;
import com.gangku.be.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider; // -> 서비스 계층으로 보내야 함
    private final UserRepository userRepository; // -> 서비스 계층으로 보내야 함

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequestDto,
                                                  HttpServletResponse response) {

        // 1) 토큰 발급
        TokenPair tokenPair = authService.login(loginRequestDto);

        // 2) Refresh Token -> HttpOnly Cookie로 설정
        ResponseCookie cookie = ResponseCookie.from("refresh_token", tokenPair.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(60 * 60 * 24 * 14) // 설계에 적은대로 14일 기준으로 수정
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 3) Access Token,  -> Response Body로 반환
        return ResponseEntity.ok(LoginResponseDto.from(tokenPair.accessToken()));
    }

    @PostMapping("/reissue")
    public ResponseEntity<LoginResponseDto> reissue(HttpServletRequest request) {

        String refreshToken = extractRefreshTokenFromCookie(request);
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 토큰에서 userId(Long 형태) 추출
        String userIdStr = jwtTokenProvider.getUserIdFromToken(refreshToken);
        Long userId = Long.parseLong(userIdStr);

        // DB 에서 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // DB의 리프레시 토큰과 일치하는지 확인
        if (!Objects.equals(refreshToken, user.getRefreshToken())) {
            throw new IllegalArgumentException("토큰이 일치하지 않습니다.");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(String.valueOf(userId));

        return ResponseEntity.ok(LoginResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // 그대로 유지
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValidity())
                .build()
        );
    }
    // 쿠키에서 리프레시 토큰을 꺼내는 메서드
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if ("refresh_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 로그아웃 처리
     * 1. RefreshToken 검증
     * 2. DB 토큰 제거
     * 3. 쿠키 삭제
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // 1. 쿠키에서 refresh_token 추출
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.badRequest().body("유효하지 않은 요청입니다.");
        }

        // 토큰에서 userId 추출
        String userIdStr = jwtTokenProvider.getUserIdFromToken(refreshToken);
        Long userId = Long.parseLong(userIdStr);

        // DB에서 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));


        // 3. DB에 저장된 리프레시 토큰 제거
        user.setRefreshToken(null);
        user.setRefreshExpiry(null);
        userService.save(user); // 변경된 유저 정보 저장

        // 4. 쿠키 삭제
        ResponseCookie deleteCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0) // 쿠키 삭제
                .build();
        response.setHeader("Set-Cookie", deleteCookie.toString());

        return ResponseEntity.ok().body("로그아웃 되었습니다.");
    }
}