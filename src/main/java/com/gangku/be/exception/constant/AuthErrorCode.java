package com.gangku.be.exception.constant;

import com.gangku.be.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {

    // --- 로그인 / 리프레시 토큰, 엑세스 토큰 재발급 / 로그아웃 ---
    REFRESH_TOKEN_NOT_FOUND(
            "REFRESH_TOKEN_NOT_FOUND", "리프레시 토큰이 없습니다.", HttpStatus.BAD_REQUEST.value()),
    INVALID_REFRESH_TOKEN(
            "INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED.value()),
    INVALID_ACCESS_TOKEN(
            "INVALID_ACCESS_TOKEN", "유효하지 않은 엑세스 토큰입니다.", HttpStatus.UNAUTHORIZED.value()),
    TOKEN_MISMATCH(
            "TOKEN_MISMATCH", "서버에 저장된 리프레시 토큰과 일치하지 않습니다.", HttpStatus.UNAUTHORIZED.value()),

    // --- 이메일 인증 플로우 ---
    INVALID_SESSION("INVALID_SESSION", "유효한 가입 세션이 없습니다.", HttpStatus.BAD_REQUEST.value()),
    INVALID_EMAIL_VERIFICATION_TOKEN(
            "INVALID_EMAIL_VERIFICATION_TOKEN",
            "유효하지 않은 이메일 인증 토큰입니다.",
            HttpStatus.UNAUTHORIZED.value()),
    EMAIL_TOKEN_EXPIRED(
            "EMAIL_TOKEN_EXPIRED", "이메일 인증 토큰이 만료되거나 이미 사용되었습니다.", HttpStatus.GONE.value()),
    EMAIL_NOT_VERIFIED(
            "EMAIL_NOT_VERIFIED", "이메일 인증이 성공적으로 완료되지 않았습니다.", HttpStatus.FORBIDDEN.value()),
    INVALID_EMAIL_VERIFICATION_SESSION(
            "INVALID_EMAIL_VERIFICATION_SESSION",
            "인증된 이메일 정보가 일치하지 않습니다.",
            HttpStatus.UNAUTHORIZED.value());

    private final String code;
    private final String message;
    private final int status;

    AuthErrorCode(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getStatus() {
        return status;
    }
}
