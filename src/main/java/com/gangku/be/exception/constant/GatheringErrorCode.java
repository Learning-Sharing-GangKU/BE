package com.gangku.be.exception.constant;

import com.gangku.be.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum GatheringErrorCode implements ErrorCode {
    FORBIDDEN("FORBIDDEN", "해당 모임의 호스트가 아닙니다.", HttpStatus.FORBIDDEN.value()),
    GATHERING_NOT_FOUND("GATHERING_NOT_FOUND", "해당 모임을 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    AI_SERVICE_UNAVAILABLE(
            "AI_SERVICE_UNAVAILABLE",
            "AI 모델 서버와의 통신에 실패했습니다.",
            HttpStatus.INTERNAL_SERVER_ERROR.value());

    private final String code;
    private final String message;
    private final int status;

    GatheringErrorCode(String code, String message, int status) {
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
