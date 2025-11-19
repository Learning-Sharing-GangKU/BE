package com.gangku.be.dto.object;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PresignResponseDto {
    private String objectKey;
    private String uploadUrl;
    private String fileUrl;
    private int expiresIn;
}
