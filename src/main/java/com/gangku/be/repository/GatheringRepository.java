package com.gangku.be.repository;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
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
}
