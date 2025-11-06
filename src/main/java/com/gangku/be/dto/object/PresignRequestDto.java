package com.gangku.be.dto.object;

import jakarta.validation.constraints.NotBlank;

public record PresignRequestDto(
    @NotBlank String fileName,
    @NotBlank String fileType
) {}