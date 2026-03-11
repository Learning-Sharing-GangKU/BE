package com.gangku.be.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequestDto {

    private String profileImageObjectKey;

    @Size(min = 1, max = 20)
    private String nickname;

    @Min(14)
    @Max(120)
    private Integer age;

    @Pattern(regexp = "^(MALE|FEMALE)$")
    private String gender;

    private Integer enrollNumber;

    @Size(max = 3)
    private List<String> preferredCategories;
}
