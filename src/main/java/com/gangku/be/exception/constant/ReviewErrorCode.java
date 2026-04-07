package com.gangku.be.exception.constant;

import com.gangku.be.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ReviewErrorCode implements ErrorCode {
    INVALID_REVIEW_TARGET(
            "INVALID_REVIEW_TARGET", "본인의 리뷰는 남길 수 없습니다.", HttpStatus.BAD_REQUEST.value()),
    NO_PERMISSION_TO_WRITE_REVIEW(
            "NO_PERMISSION_TO_WRITE_REVIEW", "리뷰를 작성할 수 없습니다.", HttpStatus.FORBIDDEN.value()),
    REVIEW_ALREADY_EXISTS(
            "REVIEW_ALREADY_EXISTS", "이미 해당 사용자에 대해 리뷰를 작성했습니다.", HttpStatus.CONFLICT.value()),
    INVALID_REVIEW_COMMENT(
            "INVALID_REVIEW_COMMENT", "리뷰 본문에 부적잘한 단어가 들어가있습니다.", HttpStatus.BAD_REQUEST.value());

    private final String code;
    private final String message;
    private final int status;

    ReviewErrorCode(String code, String message, int status) {
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
