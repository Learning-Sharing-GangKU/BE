package com.gangku.be.exception.constant;

import com.gangku.be.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum UserErrorCode implements ErrorCode {

    // --- 회원가입 ---
    USER_NOT_FOUND(
            "USER_NOT_FOUND",
            "사용자를 찾을 수 없습니다.",
            HttpStatus.NOT_FOUND.value()
    ),
    INVALID_EMAIL_FORMAT(
            "INVALID_EMAIL_FORMAT",
            "이메일 형식이 올바르지 않습니다.",
            HttpStatus.BAD_REQUEST.value()
    ),
    PASSWORD_TOO_WEAK(
            "PASSWORD_TOO_WEAK",
            "비밀번호 규칙을 확인해주세요.",
            HttpStatus.BAD_REQUEST.value()
    ),
    EMAIL_ALREADY_EXISTS(
            "EMAIL_ALREADY_EXISTS",
            "이미 사용 중인 이메일입니다.",
            HttpStatus.CONFLICT.value()
    ),
    NICKNAME_ALREADY_EXISTS(
            "NICKNAME_ALREADY_EXISTS",
            "이미 사용 중인 닉네임입니다.",
            HttpStatus.CONFLICT.value()
    );

    private final String code;
    private final String message;
    private final int status;

    UserErrorCode(String code, String message, int status) {
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
