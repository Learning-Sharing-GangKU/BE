package com.gangku.be.dto.review;

import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.domain.Review;
import com.gangku.be.model.common.PrefixedId;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ReviewCreateResponseDto {
    private String reviewId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    public static ReviewCreateResponseDto from(Review review) {
        String publicReviewId =
                PrefixedId.of(ResourceType.REVIEW, review.getId()).toExternal();

        return ReviewCreateResponseDto.builder()
                .reviewId(publicReviewId)
                .rating(review.getRating())
                .comment(review.getContent())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
