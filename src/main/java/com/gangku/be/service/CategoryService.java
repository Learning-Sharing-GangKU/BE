package com.gangku.be.service;

import com.gangku.be.domain.Category;
import com.gangku.be.repository.CategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(Category::getName)
                .toList();
    }
}
