package com.gangku.be.controller;

import com.gangku.be.service.ObjectStoragePresignService.BadRequestApiException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Getter
@AllArgsConstructor
class ApiErrorBody {
    private ErrorObj error;

    @Getter
    @AllArgsConstructor
    static class ErrorObj {
        private String code;
        private String message;
    }
}

class ServerErrorApiException extends RuntimeException {
    public final String code;
    public ServerErrorApiException(String code, String message) {
        super(message);
        this.code = code;
    }
}

@RestControllerAdvice
public class ApiErrorAdvice {

    @ExceptionHandler(BadRequestApiException.class)
    public ResponseEntity<ApiErrorBody> badRequest(BadRequestApiException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorBody(new ApiErrorBody.ErrorObj(e.code, e.getMessage())));
    }

    @ExceptionHandler(ServerErrorApiException.class)
    public ResponseEntity<ApiErrorBody> server(ServerErrorApiException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorBody(new ApiErrorBody.ErrorObj(e.code, e.getMessage())));
    }
}
