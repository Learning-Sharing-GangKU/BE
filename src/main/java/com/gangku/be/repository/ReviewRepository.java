package com.gangku.be.repository;

import com.gangku.be.domain.Review;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByRevieweeId(Long revieweeId, Pageable pageable);

    @Query(
            """
            select r
            from Review r
            where r.reviewee.id = :revieweeId
            """)
    List<Review> findFirstPageByRevieweeId(@Param("revieweeId") Long revieweeId, Pageable pageable);

    @Query(
            """
            select r
            from Review r
            where r.reviewee.id = :revieweeId
              and (
                    r.createdAt < :createdAt
                    or (r.createdAt = :createdAt and r.id < :id)
              )
            order by r.createdAt desc, r.id desc
            """)
    List<Review> findNextPageByRevieweeIdAndCursorDesc(
            @Param("revieweeId") Long revieweeId,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("id") Long id,
            Pageable pageable);


    @Query("""
        select avg(r.rating)
        from Review r
        where r.reviewee.id = :revieweeId
        """)
    Double findAverageRatingByRevieweeId(@Param("revieweeId") Long revieweeId);

    Optional<Review> findByIdAndReviewerId(Long reviewId, Long reviewerId);

    boolean existsByGatheringIdAndReviewerIdAndRevieweeId(
            Long gatheringId, Long reviewerId, Long revieweeId);
}
