package com.gangku.be.dto.user;

import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.domain.User;
import com.gangku.be.model.common.PrefixedId;
import com.gangku.be.model.review.ReviewsPreview;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileResponseDto {

    private String id;
    private String profileImageUrl;
    private String nickname;
    private Integer age;
    private String gender;
    private Integer enrollNumber;
    private List<String> preferredCategories;
    private Double averageRating;
    private Boolean reviewPublic;
    private ReviewsPreview reviewsPreview;

    public static UserProfileResponseDto from(
            User user,
            String profileImageUrl,
            List<String> preferredCategories,
            Double averageRating,
            ReviewsPreview reviewsPreview) {
        String publicUserId = PrefixedId.of(ResourceType.USER, user.getId()).toExternal();
        return UserProfileResponseDto.builder()
                .id(publicUserId)
                .profileImageUrl(profileImageUrl)
                .nickname(user.getNickname())
                .age(user.getAge())
                .gender(user.getGender())
                .enrollNumber(user.getEnrollNumber())
                .preferredCategories(preferredCategories)
                .averageRating(averageRating)
                .reviewPublic(user.getReviewPublic())
                .reviewsPreview(reviewsPreview)
                .build();
    }
}
