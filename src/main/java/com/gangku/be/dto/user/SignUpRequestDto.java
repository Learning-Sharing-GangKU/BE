package com.gangku.be.dto.user;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignUpRequestDto {

    @NotBlank
    @Email
    @Size(min = 5, max = 254)
    private String email;

    @NotBlank
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_])[A-Za-z\\d\\W_]{8,}$")
    private String password;

    @NotBlank
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]{2,20}$")
    @Size(min = 2, max = 20)
    private String nickname;

    @NotNull
    @Min(value = 14)
    @Max(value = 100)
    private Integer age;

    @NotBlank
    @Pattern(regexp = "^(MALE|FEMALE)$")
    private String gender;

    @NotNull
    private Integer enrollNumber;

    private String profileImageObjectKey;

    @NotNull
    @Size(max = 3)
    private List<String> preferredCategories;
}