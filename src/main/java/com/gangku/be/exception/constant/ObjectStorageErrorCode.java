package com.gangku.be.exception.constant;

import com.gangku.be.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ObjectStorageErrorCode implements ErrorCode {

    // --- Presigned URL 발급 ---
    INVALID_FILE_TYPE(
            "INVALID_FILE_TYPE",
            "지원하지 않은 파일 형식입니다. (허용: jpg, jpeg, png)",
            HttpStatus.BAD_REQUEST.value()
    ),
    UPLOAD_FORBIDDEN(
            "UPLOAD_FORBIDDEN",
            "해당 사용자에게 업로드 권한이 없습니다.",
            HttpStatus.FORBIDDEN.value()
    );

    private final String code;
    private final String message;
    private final int status;

    ObjectStorageErrorCode(String code, String message, int status) {
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
