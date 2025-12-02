package com.gangku.be.dto.user;

import com.gangku.be.model.ImageObject;
import jakarta.validation.Valid;
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
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$")
    private String password;

    @NotBlank
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]{2,20}$")
    private String nickname;

    @NotBlank
    @Min(value = 14)
    @Max(value = 100)
    private Integer age;

    @NotBlank
    @Pattern(regexp = "^(MALE|FEMALE)$")
    private String gender;

    @NotNull
    private Integer enrollNumber;

    @Valid
    private ImageObject profileImage;

    @Size(max = 3)
    private List<String> preferredCategories;
}