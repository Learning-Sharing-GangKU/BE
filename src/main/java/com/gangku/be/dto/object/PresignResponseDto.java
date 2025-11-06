package com.gangku.be.dto.object;

public record PresignResponseDto (
    String objectKey,
    String uploadUrl,
    String fileUrl,
    int    expiresIn
) {}
