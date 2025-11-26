package com.gangku.be.dto.gathering;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GatheringIntroResponseDto {

    @NotBlank
    private String intro;

    @NotBlank
    private String aiVersion;
}
