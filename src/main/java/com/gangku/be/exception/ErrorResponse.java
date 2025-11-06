package com.gangku.be.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private ErrorDetail error;

    public ErrorResponse(ErrorCode code, String message) {
        this.error = new ErrorDetail(code.name(), message);
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorDetail {
        private String code;
        private String message;
    }
}