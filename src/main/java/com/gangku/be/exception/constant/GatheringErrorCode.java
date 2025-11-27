package com.gangku.be.exception.constant;

import com.gangku.be.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum GatheringErrorCode implements ErrorCode {

    INVALID_FIELD_VALUE(
            "INVALID_FIELD_VALUE",
            "수정하려는 필드의 형식이 올바르지 않습니다.",
            HttpStatus.BAD_REQUEST.value()
    ),
    UNAUTHORIZED(
            "UNAUTHORIZED",
            "인증이 필요합니다. 유효한 액세스 토큰을 제공해주세요.",
            HttpStatus.UNAUTHORIZED.value()
    ),
    FORBIDDEN(
            "FORBIDDEN",
            "해당 모임을 수정할 권한이 없습니다.",
            HttpStatus.FORBIDDEN.value()
    ),
    GATHERING_NOT_FOUND(
            "GATHERING_NOT_FOUND",
            "해당 모임을 찾을 수 없습니다.",
            HttpStatus.NOT_FOUND.value()
    ),
    AI_SERVICE_UNAVAILABLE(
            "AI_SERVICE_UNAVAILABLE",
            "AI 모델 서버와의 통신에 실패했습니다.",
            HttpStatus.INTERNAL_SERVER_ERROR.value()
    ),
    INVALID_KEYWORD_FORMAT(
            "INVALID_KEYWORD_FORMAT",
            "키워드의 형식이 잘못되었습니다.",
            HttpStatus.BAD_REQUEST.value()
    ),
    INVALID_PARAMETER_FORMAT(
            "INVALID_PARAMETER_FORMAT",
            "파라미터의 형식이 올바르지 않습니다.",
            HttpStatus.BAD_REQUEST.value()
    );

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
