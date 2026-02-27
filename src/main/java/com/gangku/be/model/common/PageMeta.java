package com.gangku.be.model.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.stream.Collectors;

public record PageMeta(
        int page,       // 1-base
        int size,
        long totalElements,
        int totalPages,
        String sortedBy,
        boolean hasPrev,
        boolean hasNext
) {
    public static PageMeta from(Page<?> pageResult, String sortedByForSpec) {
        return new PageMeta(
                pageResult.getNumber() + 1, // 0-base -> 1-base
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                sortedByForSpec == null ? "" : sortedByForSpec,
                pageResult.hasPrevious(),
                pageResult.hasNext()
        );
    }

}
