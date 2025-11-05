package com.gangku.be.dto.category;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CategoryListResponseDto {
    private List<String> categories;
}