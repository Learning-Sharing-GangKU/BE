package com.gangku.be.repository;

import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    boolean existsByUserAndGathering(User user, Gathering gathering); // 중복 참가를 막음
    Optional<Participation> findByUserAndGathering(User user, Gathering gathering);
    Page<Participation> findByGatheringId(Long gatheringId, Pageable pageable);

    @Query("""
SELECT p.gathering
FROM Participation p
JOIN p.gathering g
JOIN g.host h
WHERE p.user.id = :userId
AND p.status = 'APPROVED'
AND p.role = 'GUEST'
ORDER BY p.joinedAt DESC, g.id DESC
""")
    Page<Gathering> findJoinedGatheringsByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );



}