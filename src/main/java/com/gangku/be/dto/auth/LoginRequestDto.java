package com.gangku.be.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequestDto {
    @NotBlank private String email;
    @NotBlank private String password;
}
