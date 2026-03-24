package com.gangku.be.repository;

import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    boolean existsByUserAndGathering(User user, Gathering gathering);

    Optional<Participation> findByUserAndGathering(User user, Gathering gathering);

    Page<Participation> findByGatheringId(Long gatheringId, Pageable pageable);

    @Query(
            """
SELECT p.gathering
FROM Participation p
JOIN p.gathering g
JOIN g.host h
WHERE p.user.id = :userId
AND p.status = 'APPROVED'
AND p.role = 'GUEST'
ORDER BY p.joinedAt DESC, g.id DESC
""")
    Page<Gathering> findJoinedGatheringsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query(
            """
select p1.gathering.id
from Participation p1
join Participation p2 on p2.gathering = p1.gathering
where p1.user.id = :reviewerId
  and p2.user.id = :revieweeId
  and p1.status = 'APPROVED'
  and p2.status = 'APPROVED'
  and p1.gathering.status = 'FINISHED'
order by p1.gathering.date desc
""")
    List<Long> findFinishedCommonGatheringIds(Long reviewerId, Long revieweeId);

    // user 마다 지금까지 방 참여한 횟수 count 용
    @Query(
            """
    SELECT p.user.id, COUNT(p)
    FROM Participation p
    WHERE p.status = 'APPROVED'
    GROUP BY p.user.id
""")
    List<Object[]> countApprovedParticipationGroupByUserId();
}
