package com.gangku.be.exception.constant;

import com.gangku.be.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum UserErrorCode implements ErrorCode {

    // --- 회원가입 ---
    USER_NOT_FOUND("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    EMAIL_CONFLICT("EMAIL_CONFLICT", "이미 가입된 이메일이 있습니다.", HttpStatus.CONFLICT.value()),
    INVALID_CREDENTIAL(
            "INVALID_CREDENTIAL", "이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED.value()),
    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT.value()),
    NICKNAME_ALREADY_EXISTS(
            "NICKNAME_ALREADY_EXISTS", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT.value()),

    // --- 회원탈퇴 ---
    NO_PERMISSION_TO_CANCEL_MEMBERSHIP("NO_PERMISSION_TO_CANCEL_MEMBERSHIP", "회원탈퇴를 할 권한이 없습니다.", HttpStatus.FORBIDDEN.value());

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
