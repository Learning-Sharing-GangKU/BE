package com.gangku.be.repository;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GatheringRepository extends JpaRepository<Gathering, Long> {

    @Query("""
    SELECT g FROM Gathering g
    JOIN FETCH g.host h
    WHERE (:category IS NULL OR g.category = :category)
    ORDER BY g.createdAt DESC, g.id DESC
""") List<Gathering> findLatestGatherings(@Param("category") Category category, Pageable pageable);

    @Query("""
    SELECT g FROM Gathering g
    JOIN FETCH g.host h
    WHERE (:category IS NULL OR g.category = :category)
    ORDER BY g.participantCount DESC, g.id DESC
""")
    List<Gathering> findPopularGatherings(@Param("category") Category category, Pageable pageable);

    @Query("""
SELECT g
FROM Gathering g
JOIN FETCH g.host h
WHERE g.host.id = :hostId
ORDER BY g.createdAt DESC, g.id DESC
""")
    List<Gathering> findByHostIdOrderByCreatedAtDesc(
            @Param("hostId") Long hostId,
            Pageable pageable
    );


}