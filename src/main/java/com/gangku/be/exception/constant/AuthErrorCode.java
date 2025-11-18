package com.gangku.be.exception.constant;

import com.gangku.be.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {

    // --- 로그인 / 리프레시 토큰 재발급 / 로그아웃 ---
    INVALID_CREDENTIAL(
            "INVALID_CREDENTIAL",
            "이메일 또는 비밀번호가 올바르지 않습니다.",
            HttpStatus.BAD_REQUEST.value()
    ),
    USER_NOT_FOUND(
            "USER_NOT_FOUND",
            "사용자를 찾을 수 없습니다.",
            HttpStatus.NOT_FOUND.value()
    ),
    REFRESH_TOKEN_NOT_FOUND(
            "REFRESH_TOKEN_NOT_FOUND",
            "리프레시 토큰이 없습니다.",
            HttpStatus.BAD_REQUEST.value()
    ),
    INVALID_REFRESH_TOKEN(
            "INVALID_REFRESH_TOKEN",
            "유효하지 않은 리프레시 토큰입니다.",
            HttpStatus.UNAUTHORIZED.value()
    ),
    TOKEN_MISMATCH(
            "TOKEN_MISMATCH",
            "서버에 저장된 리프레시 토큰과 일치하지 않습니다.",
            HttpStatus.FORBIDDEN.value()
    ),
    UNAUTHORIZED(
            "UNAUTHORIZED",
            "로그인 정보가 유효하지 않습니다.",
            HttpStatus.UNAUTHORIZED.value()
    ),

    // --- 이메일 인증 플로우 ---
    INVALID_EMAIL_FORMAT(
            "INVALID_EMAIL_FORMAT",
            "이메일 형식이 올바르지 않습니다.",
            HttpStatus.BAD_REQUEST.value()
    ),
    EMAIL_CONFLICT(
            "EMAIL_CONFLICT",
            "이미 가입된 이메일이 있습니다.",
            HttpStatus.CONFLICT.value()
    ),
    INVALID_TOKEN_FORMAT(
            "INVALID_TOKEN_FORMAT",
            "유효하지 않은 토큰 형식입니다.",
            HttpStatus.BAD_REQUEST.value()
    ),
    USER_NOT_FOUND_BY_TOKEN(
            "USER_NOT_FOUND_BY_TOKEN",
            "해당 토큰에 해당하는 사용자를 찾을 수 없습니다.",
            HttpStatus.NOT_FOUND.value()
    ),
    TOKEN_EXPIRED(
            "TOKEN_EXPIRED",
            "이메일 인증 토큰이 만료되었습니다.",
            HttpStatus.GONE.value()
    ),
    INVALID_SESSION(
            "INVALID_SESSION",
            "유효한 가입 세션이 없습니다.",
            HttpStatus.BAD_REQUEST.value()
    ),
    VERIFICATION_NOT_STARTED(
            "VERIFICATION_NOT_STARTED",
            "인증 메일 발송 기록이 없습니다.",
            HttpStatus.BAD_REQUEST.value()
    ),
    EMAIL_MISMATCH(
            "EMAIL_MISMATCH",
            "세션의 이메일과 이늦ㅇ된 이메일이 일치하지 않습니다.",
            HttpStatus.BAD_REQUEST.value()
    ),
    TOKEN_EXPIRED_OR_USED(
            "TOKEN_EXPRIED_OR_USED",
            "인증 토큰이 만료되었거나 이미 사용되었습니다.",
            HttpStatus.GONE.value()
    );

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
