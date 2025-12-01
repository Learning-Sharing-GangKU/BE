package com.gangku.be.model;

import jakarta.validation.constraints.NotBlank;

public record ImageObject(
        @NotBlank
        String bucket,

        @NotBlank
        String key
) { }
