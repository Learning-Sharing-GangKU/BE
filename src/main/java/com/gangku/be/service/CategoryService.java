package com.gangku.be.service;

import com.gangku.be.domain.Category;
import com.gangku.be.dto.category.CategoryDto;
import com.gangku.be.dto.category.CategoryResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.CustomExceptionOld;
import com.gangku.be.exception.ErrorCode;
import com.gangku.be.exception.ErrorCodeOld;
import com.gangku.be.exception.constant.CategoryErrorCode;
import com.gangku.be.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
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