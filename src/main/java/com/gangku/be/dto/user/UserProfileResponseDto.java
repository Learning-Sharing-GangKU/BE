package com.gangku.be.dto.user;

import com.gangku.be.domain.User;
import java.util.List;

import com.gangku.be.model.review.ReviewsPreview;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileResponseDto {

    private Long id;
    private String profileImageUrl;
    private String nickname;
    private Integer age;
    private String gender;
    private Integer enrollNumber;
    private List<String> preferredCategories;
    private Boolean reviewsPublic;
    private ReviewsPreview reviewsPreview;

    public static UserProfileResponseDto from(
            User user,
            String profileImageUrl,
            List<String> preferredCategories,
            ReviewsPreview reviewsPreview
    ) {
        return UserProfileResponseDto.builder()
                .id(user.getId())
                .profileImageUrl(profileImageUrl)
                .nickname(user.getNickname())
                .age(user.getAge())
                .gender(user.getGender())
                .enrollNumber(user.getEnrollNumber())
                .preferredCategories(preferredCategories)
                .reviewsPublic(user.getReviewsPublic())
                .reviewsPreview(reviewsPreview)
                .build();
    }


}