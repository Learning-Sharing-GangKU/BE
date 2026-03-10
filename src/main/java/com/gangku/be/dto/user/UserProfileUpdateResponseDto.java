package com.gangku.be.dto.user;

import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.domain.User;
import com.gangku.be.model.common.PrefixedId;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileUpdateResponseDto {

    private String id;
    private String profileImageUrl;
    private String nickname;
    private Integer age;
    private String gender;
    private Integer enrollNumber;
    private List<String> preferredCategories;
    private LocalDateTime updatedAt;

    public static UserProfileUpdateResponseDto from(
            User user,
            String profileImageUrl,
            List<String> preferredCategories
    ) {

        String publicUserId =
                PrefixedId.of(ResourceType.USER, user.getId()).toExternal();

        return UserProfileUpdateResponseDto.builder()
                .id(publicUserId)
                .profileImageUrl(profileImageUrl)
                .nickname(user.getNickname())
                .age(user.getAge())
                .gender(user.getGender())
                .enrollNumber(user.getEnrollNumber())
                .preferredCategories(preferredCategories)
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}