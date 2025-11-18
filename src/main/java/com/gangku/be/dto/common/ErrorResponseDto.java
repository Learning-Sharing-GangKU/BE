package com.gangku.be.dto.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponseDto {

    private ErrorDetail error;

    @Getter
    @AllArgsConstructor
    public static class ErrorDetail {
        private String code;
        private String message;
    }

    public static ErrorResponseDto of(String code, String message) {
        return new ErrorResponseDto(new ErrorDetail(code, message));
    }
}