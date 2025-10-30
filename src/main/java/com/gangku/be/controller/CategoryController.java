package com.gangku.be.controller;

import com.gangku.be.domain.Category;
import com.gangku.be.dto.category.CategoryDto;
import com.gangku.be.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(name));
    }
}