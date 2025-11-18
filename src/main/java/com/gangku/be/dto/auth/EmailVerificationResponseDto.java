package com.gangku.be.dto.auth;

import com.gangku.be.model.EmailVerificationConfirmResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class EmailVerificationResponseDto {
    private boolean verified;
    private String email;

    public static EmailVerificationResponseDto from(EmailVerificationConfirmResult confirmResult) {
        return EmailVerificationResponseDto.builder()
                .verified(confirmResult.verified())
                .email(confirmResult.email())
                .build();
    }

    public static LoginResponseDto from(String accessToken) {
        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .build();
    }
}
