package com.gangku.be.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class LoginResponseDto {
    private String accessToken;
    private String tokenType;

    public static LoginResponseDto from(String accessToken) {
        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .build();
    }
}