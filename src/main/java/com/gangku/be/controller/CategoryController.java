package com.gangku.be.controller;

import com.gangku.be.dto.category.CategoryResponseDto;
import com.gangku.be.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<CategoryResponseDto> getCategories() {

        List<String> categoryNames = categoryService.getAllCategories();

        return ResponseEntity.ok(new CategoryResponseDto(categoryNames));
    }
}
