package com.gangku.be.exception.constant;

import com.gangku.be.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CategoryErrorCode implements ErrorCode {

    CATEGORY_NOT_FOUND(
            "CATEGORY_NOT_FOUND",
            "카테고리 목록을 찾을 수 없습니다.",
            HttpStatus.NOT_FOUND.value()
    );

    private final String code;
    private final String message;
    private final int status;

    CategoryErrorCode(String code, String message, int status) {
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
