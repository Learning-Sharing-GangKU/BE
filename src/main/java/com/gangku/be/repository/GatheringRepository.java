package com.gangku.be.repository;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Gathering.Status;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GatheringRepository extends JpaRepository<Gathering, Long> {

    @Query(
            value = """
            SELECT g
            FROM Gathering g
            JOIN FETCH g.host h
            WHERE (:category IS NULL OR g.category = :category)
            ORDER BY g.createdAt DESC, g.id DESC
        """,
            countQuery = """
            SELECT COUNT(g)
            FROM Gathering g
            WHERE (:category IS NULL OR g.category = :category)
        """
    )
    Page<Gathering> findLatestGatherings(
            @Param("category") Category category,
            Pageable pageable
    );

    @Query(
            value = """
            SELECT g
            FROM Gathering g
            JOIN FETCH g.host h
            WHERE (:category IS NULL OR g.category = :category)
            ORDER BY g.participantCount DESC, g.id DESC
        """,
            countQuery = """
            SELECT COUNT(g)
            FROM Gathering g
            WHERE (:category IS NULL OR g.category = :category)
        """
    )
    Page<Gathering> findPopularGatherings(
            @Param("category") Category category,
            Pageable pageable
    );

    @Query(
            value = """
            SELECT g
            FROM Gathering g
            JOIN FETCH g.host h
            WHERE g.host.id = :hostId
            ORDER BY g.createdAt DESC, g.id DESC
        """,
            countQuery = """
            SELECT COUNT(g)
            FROM Gathering g
            WHERE g.host.id = :hostId
        """
    )
    Page<Gathering> findByHostIdOrderByCreatedAtDesc(
            @Param("hostId") Long hostId,
            Pageable pageable
    );

    // AI 후보용: 모집중인 방 중 최신 50개
    List<Gathering> findTop50ByStatusOrderByCreatedAtDesc(Status status);

    // AI가 추천해준 ID 리스트로 조회
    List<Gathering> findByIdIn(Collection<Long> ids);
}
