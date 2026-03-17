package com.gangku.be.dto.user;

import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.domain.User;
import com.gangku.be.model.common.PrefixedId;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UpdateReviewSettingResponseDto {

    private String userId;
    private Boolean reviewPublic;
    private LocalDateTime updatedAt;

    public static UpdateReviewSettingResponseDto from(User user) {
        String publicUserId = PrefixedId.of(ResourceType.USER, user.getId()).toExternal();

        return UpdateReviewSettingResponseDto.builder()
                .userId(publicUserId)
                .reviewPublic(user.getReviewsPublic())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
