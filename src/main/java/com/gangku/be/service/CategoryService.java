package com.gangku.be.service;

import com.gangku.be.domain.Category;
import com.gangku.be.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<String> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(Category::getName)
                .toList();
    }
}