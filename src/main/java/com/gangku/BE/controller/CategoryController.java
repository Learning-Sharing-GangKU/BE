package com.gangku.BE.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CategoryController {

    @GetMapping("/categories")
    public ResponseEntity<Map<String, List<String>>> getCategories() {
        // 실제 DB나 ENUM에서 읽어올 수도 있음
        List<String> categories = List.of("movie", "game", "music", "travel", "study");
        // 프론트가 기대하는 형식으로 변환
        return ResponseEntity.ok(Map.of("categories", categories));
    }
}