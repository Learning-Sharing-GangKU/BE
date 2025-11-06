package com.gangku.be.dto.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

//에러 응답용 Dto

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
}