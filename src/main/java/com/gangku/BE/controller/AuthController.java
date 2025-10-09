package com.gangku.BE.controller;

import com.gangku.BE.domain.User;
import com.gangku.BE.dto.*;
import com.gangku.BE.jwt.JwtTokenProvider;
import com.gangku.BE.service.UserService;
import com.gangku.BE.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginDto,
                                                  HttpServletResponse response) {
        // loginResponse 내부에서 authenticate + 토큰 생성 + 저장
        LoginResponseDto loginResponse = userService.login(loginDto);
        User user = userService.authenticate(loginDto.getEmail(), loginDto.getPassword());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());
        long expiresIn = 60 * 60; // 1시간 = 3600초 (Access Token 유효시간)

        // HttpOnly 쿠키 설정
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(60 * 60 * 24 * 7)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/reissue")
    public ResponseEntity<LoginResponseDto> reissue(HttpServletRequest request) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // DB의 리프레시 토큰과 일치하는지 확인
        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new IllegalArgumentException("토큰이 일치하지 않습니다.");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUserId());

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

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // 1. 쿠키에서 refresh_token 추출
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.badRequest().body("유효하지 않은 요청입니다.");
        }

        // 2. 해당 유저 찾기
        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userService.findByUserId(userId);

        // 3. DB에 저장된 리프레시 토큰 제거
        user.setRefreshToken(null);
        user.setRefreshExpiry(null);
        userService.save(user); // 변경된 유저 정보 저장

        // 4. 쿠키 삭제 (Set-Cookie with maxAge=0)
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