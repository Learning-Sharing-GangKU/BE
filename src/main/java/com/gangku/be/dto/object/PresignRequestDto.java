package com.gangku.be.dto.object;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PresignRequestDto {
    @NotBlank private String fileName;
    @NotBlank private String fileType;
}
