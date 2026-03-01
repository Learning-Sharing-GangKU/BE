package com.gangku.be.dto.preferred;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PreferredCategoryRequestDto {
    private List<String> categoryNames;
}
