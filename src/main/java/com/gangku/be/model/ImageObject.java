package com.gangku.be.model;

public record ImageObject(
        String bucket,

        String key
) {
    public String asPath() {
        return bucket + "/" + key;
    }

    public static String toPathOrNull(ImageObject imageObject) {
        return imageObject == null ? null : imageObject.asPath();
    }
}
