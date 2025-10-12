// src/main/java/com/gangku/BE/dto/SignupRequestDto.java

package com.gangku.be.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SignupRequestDto {

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(min = 5, max = 254)
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    private String password;

    @NotBlank
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]{2,20}$", message = "닉네임은 한글, 영문, 숫자 2~20자여야 합니다.")
    private String nickname;

    @Min(value = 14, message = "나이는 14세 이상이어야 합니다.")
    @Max(value = 100, message = "나이는 100세 이하여야 합니다.")
    private Integer age;

    @NotBlank
    @Pattern(regexp = "^(MALE|FEMALE)$", message = "성별은 MALE 또는 FEMALE 이어야 합니다.")
    private String gender;

    @NotNull
    private Integer enrollNumber;

//    @NotNull
    private ProfileImage profileImage;

    @Size(max = 3, message = "선호 카테고리는 최대 3개까지만 선택 가능합니다.")
    private List<String> preferredCategories;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProfileImage {
//        @NotBlank
        private String bucket;

//        @NotBlank
        private String key;
    }
}