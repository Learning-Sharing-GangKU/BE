package com.gangku.be.repository;

import com.gangku.be.constant.gathering.GatheringStatus;
import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.User;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GatheringRepository extends JpaRepository<Gathering, Long> {

    // 카테고리가 없는 최신순
    @Query(
            value =
                    """
    SELECT g
    FROM Gathering g
    JOIN FETCH g.host
    WHERE g.status <> com.gangku.be.constant.gathering.GatheringStatus.FINISHED
""",
            countQuery =
                    """
    SELECT COUNT(g)
    FROM Gathering g
    WHERE g.status <> com.gangku.be.constant.gathering.GatheringStatus.FINISHED
""")
    Page<Gathering> findLatestGatherings(Pageable pageable);

    // 카테고리가 있는 최신순
    @Query(
            value =
                    """
    SELECT g
    FROM Gathering g
    JOIN FETCH g.host
    WHERE g.category = :category
      AND g.status <> com.gangku.be.constant.gathering.GatheringStatus.FINISHED
""",
            countQuery =
                    """
    SELECT COUNT(g)
    FROM Gathering g
    WHERE g.category = :category
      AND g.status <> com.gangku.be.constant.gathering.GatheringStatus.FINISHED
""")
    Page<Gathering> findLatestGatheringsByCategory(
            @Param("category") Category category, Pageable pageable);

    // 카테고리가 없는 인기순
    @Query(
            value =
                    """
    SELECT g
    FROM Gathering g
    JOIN FETCH g.host
    WHERE g.status <> com.gangku.be.constant.gathering.GatheringStatus.FINISHED
""",
            countQuery =
                    """
    SELECT COUNT(g)
    FROM Gathering g
    WHERE g.status <> com.gangku.be.constant.gathering.GatheringStatus.FINISHED
""")
    Page<Gathering> findPopularGatherings(Pageable pageable);

    // 카테고리가 있는 인기순
    @Query(
            value =
                    """
    SELECT g
    FROM Gathering g
    JOIN FETCH g.host
    WHERE g.category = :category
      AND g.status <> com.gangku.be.constant.gathering.GatheringStatus.FINISHED
""",
            countQuery =
                    """
    SELECT COUNT(g)
    FROM Gathering g
    WHERE g.category = :category
      AND g.status <> com.gangku.be.constant.gathering.GatheringStatus.FINISHED
""")
    Page<Gathering> findPopularGatheringsByCategory(
            @Param("category") Category category, Pageable pageable);

    @Query(
            value =
                    """
        SELECT g
        FROM Gathering g
        JOIN FETCH g.host
        JOIN FETCH g.category
        WHERE g.host = :host
    """,
            countQuery =
                    """
        SELECT COUNT(g)
        FROM Gathering g
        WHERE g.host = :host
    """)
    Page<Gathering> findByHostId(@Param("host") User host, Pageable pageable);

    List<Gathering> findTop50ByStatusNotOrderByCreatedAtDesc(GatheringStatus status);

    List<Gathering> findTop50ByCategoryAndStatusNotOrderByCreatedAtDesc(
            Category category, GatheringStatus status);

    // AI가 추천해준 ID 리스트로 조회
    List<Gathering> findByIdIn(Collection<Long> ids);
}
