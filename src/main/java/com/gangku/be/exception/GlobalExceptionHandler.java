package com.gangku.be.exception;

import com.gangku.be.dto.common.ErrorResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponseDto> handleBusiness(CustomException customException) {
        ErrorCode code = customException.getErrorCode();

        ErrorResponseDto body = ErrorResponseDto.of(
                code.getCode(),
                code.getMessage()
        );

        return ResponseEntity
                .status(code.getStatus())
                .body(body);
    }
}