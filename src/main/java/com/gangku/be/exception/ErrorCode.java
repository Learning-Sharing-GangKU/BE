package com.gangku.be.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 400
    INVALID_PARAMETER_FORMAT(HttpStatus.BAD_REQUEST, "파라미터의 형식이 올바르지 않습니다."),
    INVALID_FIELD_VALUE(HttpStatus.BAD_REQUEST, "요청한 필드의 형식이 올바르지 않습니다."),
    INVALID_GATHERING_ID(HttpStatus.BAD_REQUEST, "gatheringId 형식이 올바르지 않습니다."),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "role은 hose 또는 guest 중 하나여야 합니다."),
    PASSWORD_TOO_WEAK(HttpStatus.BAD_REQUEST, "비밀번호 규칙을 확인해주세요."),
    // 401
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다. 유효한 액세스 토큰을 제공해주세요."),

    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN, "해당 요청을 수행할 권한이 없습니다."),

    // 404
    GATHERING_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 모임을 찾을 수 없습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 카테고리를 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    CATEGORIES_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리 목록을 찾을 수 없습니다."),

    // 409
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    ALREADY_LEFT(HttpStatus.CONFLICT, "모임 참여 명단에 없는 사용자입니다."),
    CAPACITY_FULL(HttpStatus.CONFLICT, "모임 정원이 가득 찼습니다"),
    ALREADY_JOINED(HttpStatus.CONFLICT, "이미 이 모임에 참여 중입니다."),

    // 500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}