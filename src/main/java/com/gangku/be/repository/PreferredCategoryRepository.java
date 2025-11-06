package com.gangku.be.repository;

import com.gangku.be.domain.PreferredCategory;
import com.gangku.be.domain.User;
import com.gangku.be.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;

/**
 * PreferredCategory 엔티티에 대한 DB 접근을 위한 JPA 레포지토리
 * - 사용자별 선호 카테고리 조회
 * - 특정 유저-카테고리 조합 존재 여부 확인
 */
public interface PreferredCategoryRepository extends JpaRepository<PreferredCategory, Long> {
    List<PreferredCategory> findByUser(User user);
    Optional<PreferredCategory> findByUserAndCategory(User user, Category category);
    void deleteByUser(User user);

    User id(Long id);
}