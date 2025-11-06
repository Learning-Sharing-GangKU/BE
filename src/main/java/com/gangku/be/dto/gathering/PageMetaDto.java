package com.gangku.be.dto.gathering;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

//모임 상세 조회시, 참여자 정렬을 위한 Dto
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageMetaDto {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private String sortedBy;
    private boolean hasPrev;
    private boolean hasNext;

    public static PageMetaDto of(int page, int size, long totalElements, String sortedBy) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return PageMetaDto.builder()
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .sortedBy(sortedBy)
                .hasPrev(page > 1)
                .hasNext(page < totalPages)
                .build();
    }
}