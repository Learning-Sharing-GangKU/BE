package com.gangku.be.model.review;

import com.gangku.be.domain.Review;
import java.util.List;
import java.util.function.Function;

public record ReviewsPreview(List<ReviewsPreviewItem> data, ReviewCursorMeta meta) {

    public static ReviewsPreview from(
            List<Review> fetchedReviews,
            int previewSize,
            String sortedBy,
            Function<Review, String> profileImageResolver) {

        boolean hasNext = fetchedReviews.size() > previewSize;

        List<Review> previewReviews = fetchedReviews.stream().limit(previewSize).toList();

        String nextCursor = null;
        if (hasNext && !previewReviews.isEmpty()) {
            Review lastReview = previewReviews.get(previewReviews.size() - 1);
            nextCursor =
                    ReviewCursorCodec.encode(
                            new ReviewCursor(lastReview.getCreatedAt(), lastReview.getId()));
        }

        List<ReviewsPreviewItem> items =
                previewReviews.stream()
                        .map(r -> ReviewsPreviewItem.from(r, profileImageResolver.apply(r)))
                        .toList();

        ReviewCursorMeta meta = ReviewCursorMeta.of(items.size(), sortedBy, nextCursor, hasNext);

        return new ReviewsPreview(items, meta);
    }
}
