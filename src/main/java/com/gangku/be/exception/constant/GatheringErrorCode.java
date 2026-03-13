package com.gangku.be.exception.constant;

import com.gangku.be.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum GatheringErrorCode implements ErrorCode {
    NO_PERMISSION_TO_MANIPULATE_GATHERING(
            "NO_PERMISSION_TO_MANIPULATE_GATHERING",
            "해당 모임의 호스트가 아닙니다.",
            HttpStatus.FORBIDDEN.value()),
    GATHERING_NOT_FOUND("GATHERING_NOT_FOUND", "해당 모임을 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    ALREADY_FINISHED_GATHERING(
            "ALREADY_FINISHED_GATHERING", "이미 종료된 모임입니다.", HttpStatus.CONFLICT.value()),
    INVALID_GATHERING_CONTENT(
            "INVALID_GATHERING_CONTENT",
            "모임 이름 혹은 설명에 부적잘한 단어가 들어가있습니다.",
            HttpStatus.BAD_REQUEST.value());

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
