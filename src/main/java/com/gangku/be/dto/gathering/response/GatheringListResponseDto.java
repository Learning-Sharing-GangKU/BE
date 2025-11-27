package com.gangku.be.dto.gathering.response;

import com.gangku.be.model.PageMeta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatheringListResponseDto {
    private List<GatheringListItemDto> data;
    private PageMeta meta;
}