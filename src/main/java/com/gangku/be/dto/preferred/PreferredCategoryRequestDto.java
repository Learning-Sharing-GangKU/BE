package com.gangku.be.dto.preferred;

import lombok.Getter;

import java.util.List;

@Getter
public class PreferredCategoryRequestDto {
    private List<String> categoryNames;
}