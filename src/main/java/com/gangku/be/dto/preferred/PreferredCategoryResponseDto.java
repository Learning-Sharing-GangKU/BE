package com.gangku.be.dto.preferred;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PreferredCategoryResponseDto {
    private List<String> preferredCategories;
}