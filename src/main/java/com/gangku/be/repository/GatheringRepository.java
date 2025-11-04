package com.gangku.be.repository;

import com.gangku.be.domain.Gathering;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GatheringRepository extends JpaRepository<Gathering, Long> {
    // 기본 CRUD 제공됨
}