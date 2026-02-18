package com.gangku.be.repository;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Gathering.Status;
import java.util.Collection;
import java.util.List;

import com.gangku.be.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GatheringRepository extends JpaRepository<Gathering, Long> {

    // 카테고리가 없는 최신순
    @Query(
            value = """
        SELECT g
        FROM Gathering g
        JOIN FETCH g.host
    """,
            countQuery = """
        SELECT COUNT(g)
        FROM Gathering g
    """
    )
    Page<Gathering> findLatestGatherings(Pageable pageable);

    // 카테고리가 있는 최신순
    @Query(
            value = """
        SELECT g
        FROM Gathering g
        JOIN FETCH g.host
        WHERE g.category = :category
    """,
            countQuery = """
        SELECT COUNT(g)
        FROM Gathering g
        WHERE g.category = :category
    """
    )
    Page<Gathering> findLatestGatheringsByCategory(
            @Param("category") Category category,
            Pageable pageable
    );



    // 카테고리가 없는 인기순
    @Query(
            value = """
    SELECT g
    FROM Gathering g
    JOIN FETCH g.host
  """,
            countQuery = """
    SELECT COUNT(g) FROM Gathering g
  """
    )
    Page<Gathering> findPopularGatherings(Pageable pageable);

    // 카테고리가 있는 인기순
    @Query(
            value = """
    SELECT g
    FROM Gathering g
    JOIN FETCH g.host
    WHERE g.category = :category
  """,
            countQuery = """
    SELECT COUNT(g)
    FROM Gathering g
    WHERE g.category = :category
  """
    )
    Page<Gathering> findPopularGatheringsByCategory(
            @Param("category") Category category,
            Pageable pageable
    );


    @Query(
            value = """
        SELECT g
        FROM Gathering g
        JOIN FETCH g.host
        JOIN FETCH g.category
        WHERE g.host = :host
        ORDER BY g.createdAt DESC, g.id DESC
    """,
            countQuery = """
        SELECT COUNT(g)
        FROM Gathering g
        WHERE g.host = :host
    """
    )
    Page<Gathering> findByHostIdOrderByCreatedAtDesc(
            @Param("host") User host,
            Pageable pageable
    );

    // AI 후보용: 모집중인 방 중 최신 50개
    List<Gathering> findTop50ByStatusOrderByCreatedAtDesc(Status status);

    // AI가 추천해준 ID 리스트로 조회
    List<Gathering> findByIdIn(Collection<Long> ids);
}
