package com.gangku.be.dto.preferred;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PreferredCategoryRequestDto {
    private List<String> categoryNames;
}