package com.gangku.be.controller;

import com.gangku.be.constant.auth.TokenProperty;
import com.gangku.be.dto.user.LoginRequestDto;
import com.gangku.be.dto.user.LoginResponseDto;
import com.gangku.be.security.jwt.TokenPair;
import com.gangku.be.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequestDto,
                                                  HttpServletResponse response) {

        // 1) 토큰 발급
        TokenPair tokenPair = authService.login(loginRequestDto);

        // 2) Refresh Token -> HttpOnly Cookie로 설정
        setRefreshTokenCookie(response, tokenPair.refreshToken());

        // 3) Access Token -> Response Body로 반환
        return ResponseEntity.ok(LoginResponseDto.from(tokenPair.accessToken()));
    }

    @PostMapping("/reissue")
    public ResponseEntity<LoginResponseDto> reIssue(HttpServletRequest request, HttpServletResponse response) {

        // 1) 새 토큰 발급
        TokenPair tokenPair = authService.reIssue(request);

        // 2) Refresh Token -> HttpOnly Cookie로 설정
        setRefreshTokenCookie(response, tokenPair.refreshToken());

        // 3) Access Token -> Response Body로 반환
        return ResponseEntity.ok(LoginResponseDto.from(tokenPair.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {

        // 1) 로그 아웃 처리
        authService.logout(request);

        // 2) Refresh Token Cookie에서 지우기
        clearRefreshTokenCookie(response);

        return ResponseEntity.noContent().build();
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {

        buildAndSetCookie(response, refreshToken, TokenProperty.REFRESH_TOKEN.getExpirationInSeconds());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {

        buildAndSetCookie(response, "", 0L);
    }

    private static void buildAndSetCookie(HttpServletResponse response, String refreshToken, Long maxAge) {

        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}