package com.gangku.be.service;

import com.gangku.be.domain.Category;
import com.gangku.be.dto.category.CategoryDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.CustomExceptionOld;
import com.gangku.be.exception.ErrorCode;
import com.gangku.be.exception.ErrorCodeOld;
import com.gangku.be.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryDto> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();

        if (categories.isEmpty()) {
            throw new CustomExceptionOld(ErrorCodeOld.CATEGORIES_NOT_FOUND);
        }

        return categories.stream()
                .map(CategoryDto::new)
                .collect(Collectors.toList());
    }

    public Category createCategory(String name) {
        if (categoryRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 카테고리입니다.");
        }
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }
}