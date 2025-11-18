package com.gangku.be.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class EmailVerificationRequestDto {

    @NotBlank
    @Email
    @Size(min = 5, max = 254)
    private String email;
}
