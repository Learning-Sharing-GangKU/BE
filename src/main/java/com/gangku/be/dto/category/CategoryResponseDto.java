package com.gangku.be.dto.category;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class CategoryResponseDto {
    private List<String> categories;
}
