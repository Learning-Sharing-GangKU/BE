package com.gangku.be.exception;

public class CustomExceptionOld extends RuntimeException {
    private final ErrorCodeOld errorCodeOld;

    public CustomExceptionOld(ErrorCodeOld errorCodeOld) {
        super(errorCodeOld.getDefaultMessage());
        this.errorCodeOld = errorCodeOld;
    }

    public CustomExceptionOld(ErrorCodeOld errorCodeOld, String customMessage) {
        super(customMessage);
        this.errorCodeOld = errorCodeOld;
    }
}
