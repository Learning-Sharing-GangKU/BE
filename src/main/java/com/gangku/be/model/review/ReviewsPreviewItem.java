package com.gangku.be.model.review;

import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.domain.Review;
import com.gangku.be.model.common.PrefixedId;

public record ReviewsPreviewItem(
        String id,
        String reviewerId,
        String reviewerProfileImageUrl,
        String reviewerNickname,
        Integer rating,
        String content,
        String createdAt) {

    public static ReviewsPreviewItem from(Review review, String reviewerProfileImageUrl) {
        String publicReviewId = PrefixedId.of(ResourceType.REVIEW, review.getId()).toExternal();

        String publicReviewerId =
                PrefixedId.of(ResourceType.USER, review.getReviewer().getId()).toExternal();

        return new ReviewsPreviewItem(
                publicReviewId,
                publicReviewerId,
                reviewerProfileImageUrl,
                review.getReviewer().getNickname(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt().toString());
    }
}
