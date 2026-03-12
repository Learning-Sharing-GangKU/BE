package com.gangku.be.exception.constant;

import com.gangku.be.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ErrorCode {
    INVALID_REQUEST_BODY(
            "INVALID_REQUEST_BODY", "요청 바디 값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST.value()),
    INVALID_REQUEST_PARAMETER(
            "INVALID_REQUEST_PARAMETER", "요청 파라미터 값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST.value()),
    UNAUTHORIZED("UNAUTHORIZED", "로그인 정보가 유효하지 않습니다.", HttpStatus.UNAUTHORIZED.value()),
    INTERNAL_SERVER_ERROR(
            "INTERNAL_SERVER_ERROR",
            "서버 내부에 오류가 발생했습니다.",
            HttpStatus.INTERNAL_SERVER_ERROR.value()),
    AI_SERVICE_ERROR("AI_SERVICE_ERROR", "AI 서버 내부에 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value());

    private final String code;
    private final String message;
    private final int status;

    CommonErrorCode(String code, String message, int status) {
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
