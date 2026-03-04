package com.gangku.be.repository;

import com.gangku.be.domain.Review;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByRevieweeId(Long revieweeId, Pageable pageable);

    Optional<Review> findByIdAndReviewerId(Long reviewId, Long reviewerId);

    boolean existsByGatheringIdAndReviewerIdAndRevieweeId(
            Long gatheringId, Long reviewerId, Long revieweeId);
}
