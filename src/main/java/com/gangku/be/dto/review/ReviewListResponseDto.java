package com.gangku.be.dto.review;

import com.gangku.be.model.review.ReviewCursorMeta;
import com.gangku.be.model.review.ReviewsPreview;
import com.gangku.be.model.review.ReviewsPreviewItem;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewListResponseDto {
    private List<ReviewsPreviewItem> data;
    private ReviewCursorMeta meta;

    public static ReviewListResponseDto from(ReviewsPreview reviewsPreview) {
        return ReviewListResponseDto.builder()
                .data(reviewsPreview.data())
                .meta(reviewsPreview.meta())
                .build();
    }
}
