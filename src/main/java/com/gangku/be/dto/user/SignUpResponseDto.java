package com.gangku.be.dto.user;

import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.domain.User;
import com.gangku.be.model.PrefixedId;
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
    private final String profileImageUrl;
    private final Integer age;
    private final String gender;
    private final Integer enrollNumber;
    private final List<String> preferredCategories;
    private final LocalDateTime createdAt;

    public static SignUpResponseDto from(User user, String profileImageUrl) {
        List<String> preferredCategoryNames = user.getPreferredCategories().stream()
                .map(preferredCategory -> preferredCategory.getCategory().getName())
                .toList();

        String publicUserId = PrefixedId.of(ResourceType.USER, user.getId()).toExternal();

        return SignUpResponseDto.builder()
                .id(publicUserId)
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(profileImageUrl)
                .age(user.getAge())
                .gender(user.getGender())
                .enrollNumber(user.getEnrollNumber())
                .preferredCategories(preferredCategoryNames)
                .createdAt(user.getCreatedAt())
                .build();
    }
}