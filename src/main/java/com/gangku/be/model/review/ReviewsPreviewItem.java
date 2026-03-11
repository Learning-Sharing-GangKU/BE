package com.gangku.be.model.review;

import com.gangku.be.domain.Review;

public record ReviewsPreviewItem(
        Long id,
        Long reviewerId,
        String reviewerProfileImageUrl,
        String reviewerNickname,
        Integer rating,
        String content,
        String createdAt) {

    public static ReviewsPreviewItem from(Review review, String reviewerProfileImageUrl) {
        return new ReviewsPreviewItem(
                review.getId(),
                review.getReviewer().getId(),
                reviewerProfileImageUrl,
                review.getReviewer().getNickname(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt().toString());
    }
}
