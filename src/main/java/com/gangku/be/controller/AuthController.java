package com.gangku.be.controller;

import com.gangku.be.constant.auth.CookieProperty;
import com.gangku.be.constant.auth.TokenProperty;
import com.gangku.be.dto.auth.EmailVerificationRequestDto;
import com.gangku.be.dto.auth.EmailVerificationResponseDto;
import com.gangku.be.dto.auth.LoginRequestDto;
import com.gangku.be.dto.auth.LoginResponseDto;
import com.gangku.be.model.EmailVerificationConfirmResult;
import com.gangku.be.model.EmailVerificationSendResult;
import com.gangku.be.model.TokenPair;
import com.gangku.be.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
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

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @RequestBody @Valid LoginRequestDto loginRequestDto,
            HttpServletResponse response
    ) {

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
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {

        // 1) 로그 아웃 처리
        authService.logout(request);

        // 2) Refresh Token Cookie에서 지우기
        clearRefreshTokenCookie(response);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email/verification")
    public ResponseEntity<Map<String, String>> sendEmailVerification(
            @RequestBody @Valid EmailVerificationRequestDto emailVerificationRequestDto,
            HttpServletResponse response
    ) {
        EmailVerificationSendResult emailVerificationSendResult =
                authService.sendEmailVerification(emailVerificationRequestDto.getEmail());

        setSignUpSessionCookie(response, emailVerificationSendResult);

        return ResponseEntity.ok(Map.of("message","인증 이메일이 성공적으로 발송되었습니다."));
    }

    @GetMapping("/email/verification/start")
    public ResponseEntity<Void> startEmailVerification(
            @RequestParam("token") String emailVerificationToken
    ) {
        authService.consumeEmailVerification(emailVerificationToken);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email/verification/confirm")
    public ResponseEntity<EmailVerificationResponseDto> confirmEmailVerification(
            @CookieValue(value = "signup_session", required = false) String signupSessionId
    ) {

        EmailVerificationConfirmResult confirmResult =
                authService.confirmEmailVerification(signupSessionId);

        return ResponseEntity.ok(EmailVerificationResponseDto.from(confirmResult));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        buildAndSetCookie(
                response,
                CookieProperty.REFRESH_TOKEN_COOKIE_NAME.getCookieName(),
                refreshToken,
                TokenProperty.REFRESH_TOKEN.getExpirationInSeconds());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        buildAndSetCookie(
                response,
                CookieProperty.REFRESH_TOKEN_COOKIE_NAME.getCookieName(),
                "",
                0L
        );
    }

    private void setSignUpSessionCookie(
            HttpServletResponse response,
            EmailVerificationSendResult emailVerificationSendResult
    ) {
        buildAndSetCookie(
                response,
                CookieProperty.SIGNUP_SESSION_COOKIE_NAME.getCookieName(),
                emailVerificationSendResult.sessionId(),
                emailVerificationSendResult.sessionTtlMinutes() * 60
        );
    }

    private void buildAndSetCookie(HttpServletResponse response, String cookieName, String value, long maxAge) {

        ResponseCookie cookie = ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}