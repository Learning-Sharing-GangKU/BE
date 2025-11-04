package com.gangku.be.repository;

import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    boolean existsByUserAndGathering(User user, Gathering gathering); // 중복 참가를 막음
    List<Participation> findAllByUser(User user);
    List<Participation> findAllByGathering(Gathering gathering);
    Optional<Participation> findByUserAndGathering(User user, Gathering gathering);

}