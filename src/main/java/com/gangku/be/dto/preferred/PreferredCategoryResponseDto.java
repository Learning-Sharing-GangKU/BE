package com.gangku.be.dto.preferred;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PreferredCategoryResponseDto {
    private List<String> preferredCategories;
}
