package com.gangku.be.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangku.be.dto.common.ErrorResponseDto;
import com.gangku.be.exception.constant.AuthErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {

        // 나중에 에러 코드 정리 다 하고 수정하자
        ErrorCode code = AuthErrorCode.TOKEN_MISMATCH;

        response.setStatus(code.getStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponseDto body = ErrorResponseDto.of(code.getCode(), code.getMessage());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
