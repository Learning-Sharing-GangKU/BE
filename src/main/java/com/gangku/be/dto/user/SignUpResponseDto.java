package com.gangku.be.dto.user;

import com.gangku.be.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@AllArgsConstructor
public class SignUpResponseDto {

    private final String id;
    private final String email;
    private final String nickname;
    private final String profileObjectKey;
    private final Integer age;
    private final String gender;
    private final Integer enrollNumber;
    private final List<String> preferredCategories;
    private final LocalDateTime createdAt;

    public static SignUpResponseDto from(User user) {
        List<String> preferredCategoryNames = user.getPreferredCategories().stream()
                .map(preferredCategory -> preferredCategory.getCategory().getName())
                .toList();

        return SignUpResponseDto.builder()
                .id("usr_" + user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileObjectKey(user.getProfileImageObject())
                .age(user.getAge())
                .gender(user.getGender())
                .enrollNumber(user.getEnrollNumber())
                .preferredCategories(preferredCategoryNames)
                .createdAt(user.getCreatedAt())
                .build();
    }
}