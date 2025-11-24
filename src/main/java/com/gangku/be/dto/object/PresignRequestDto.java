package com.gangku.be.dto.object;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PresignRequestDto {
    @NotBlank private String fileName;
    @NotBlank private String fileType;
}
