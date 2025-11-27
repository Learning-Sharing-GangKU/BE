package com.gangku.be.model;

import org.springframework.data.domain.Page;

public record PageMeta(
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sortedBy,
        boolean hasPrev,
        boolean hasNext
) {
    public static PageMeta from(
            Page<?> pageResult,
            int pageNumber,
            int size,
            String sortedBy
    ) {
        return new PageMeta(
                pageNumber,
                size,
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                sortedBy,
                pageResult.hasPrevious(),
                pageResult.hasNext()
        );
    }
}
