package com.gangku.be.exception.constant;

import com.gangku.be.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ParticipationErrorCode implements ErrorCode {

    ALREADY_JOINED(
            "ALREADY_JOINED",
            "이미 이 모임에 참여 중입니다.",
            HttpStatus.CONFLICT.value()
    ),
    CAPACITY_FULL(
            "CAPACITY_FULL",
            "모임 정원이 가득 찼습니다.",
            HttpStatus.CONFLICT.value()
    ),
    ALREADY_LEFT(
            "ALREADY_LEFT",
            "모임 참여 명단에 없는 사용자입니다.",
            HttpStatus.CONFLICT.value()
    ),
    HOST_CANNOT_LEAVE(
            "HOST_CANNOT_LEAVE",
            "모임장은 참여를 취소할 수 없습니다.",
            HttpStatus.CONFLICT.value()
    ),
    INVALID_PARAMETER_FORMAT(
            "INVALID_PARAMETER_FORMAT",
                    "파라미터의 형식이 올바르지 않습니다.",
            HttpStatus.BAD_REQUEST.value()
    );

    private final String code;
    private final String message;
    private final int status;

    ParticipationErrorCode(String code, String message, int status) {
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
