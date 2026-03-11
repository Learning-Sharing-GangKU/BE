package com.gangku.be.model.review;

import com.gangku.be.domain.Review;
import com.gangku.be.model.common.PageMeta;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record ReviewsPreview(List<ReviewsPreviewItem> data, PageMeta meta) {

    public static ReviewsPreview from(
            Page<Review> reviewPage,
            String sortedByForSpec,
            Function<Review, String> profileImageResolver) {

        List<ReviewsPreviewItem> items =
                reviewPage.getContent().stream()
                        .limit(3)
                        .map(r -> ReviewsPreviewItem.from(r, profileImageResolver.apply(r)))
                        .toList();

        PageMeta meta = PageMeta.from(reviewPage, sortedByForSpec);

        return new ReviewsPreview(items, meta);
    }
}
