package com.gangku.be.repository;

import com.gangku.be.domain.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name); // 중복 체크용
    List<Category> findByNameIn(List<String> names);
    List<Category> findAllByOrderByNameAsc();
}