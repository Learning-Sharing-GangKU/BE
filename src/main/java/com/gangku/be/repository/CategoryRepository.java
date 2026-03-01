package com.gangku.be.repository;

import com.gangku.be.domain.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name); // 중복 체크용

    List<Category> findByNameIn(List<String> names);

    List<Category> findAllByOrderByNameAsc();
}
