package com.gangku.be.dto.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
public class CategoryResponseDto {
    private List<String> categories;
}