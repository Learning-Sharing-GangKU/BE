package com.gangku.be.model;

import jakarta.validation.constraints.NotBlank;

public record ImageObject(
        @NotBlank
        String bucket,

        @NotBlank
        String key
) {
    public String asPath() {
        return bucket + "/" + key;
    }

    public static String toPathOrNull(ImageObject imageObject) {
        return imageObject == null ? null : imageObject.asPath();
    }
}
