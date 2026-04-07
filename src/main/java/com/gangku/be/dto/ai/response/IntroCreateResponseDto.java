package com.gangku.be.dto.ai.response;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IntroCreateResponseDto {

    @NotBlank private String intro;

    @NotBlank private String aiVersion;
}
