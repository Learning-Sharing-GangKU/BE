package com.gangku.be.exception;

import com.gangku.be.dto.common.ErrorResponseDto;
import com.gangku.be.exception.constant.CommonErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponseDto> handleCustomException(CustomException customException) {
        ErrorCode code = customException.getErrorCode();

        ErrorResponseDto body = ErrorResponseDto.of(
                code.getCode(),
                code.getMessage()
        );

        return ResponseEntity
                .status(code.getStatus())
                .body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValid(
            MethodArgumentNotValidException methodArgumentNotValidException
    ) {
        ErrorResponseDto body = ErrorResponseDto.of(
                CommonErrorCode.INVALID_REQUEST_BODY.getCode(),
                CommonErrorCode.INVALID_REQUEST_BODY.getMessage()
        );

        return ResponseEntity
                .status(CommonErrorCode.INVALID_REQUEST_BODY.getStatus())
                .body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolationException(
            ConstraintViolationException constraintViolationException
    ) {
        ErrorResponseDto body = ErrorResponseDto.of(
                CommonErrorCode.INVALID_REQUEST_PARAMETER.getCode(),
                CommonErrorCode.INVALID_REQUEST_PARAMETER.getMessage()
        );

        return ResponseEntity
                .status(CommonErrorCode.INVALID_REQUEST_PARAMETER.getStatus())
                .body(body);
    }
}