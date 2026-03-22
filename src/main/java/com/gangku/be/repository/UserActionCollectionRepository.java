package com.gangku.be.repository;

import com.gangku.be.domain.UserActionCollection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserActionCollectionRepository extends JpaRepository<UserActionCollection, Long> {
    // user action 로그 수집
    @Query("SELECT ua FROM UserActionCollection ua JOIN FETCH ua.user JOIN FETCH ua.gathering")
    List<UserActionCollection> findAllWithUserAndGathering();
}
