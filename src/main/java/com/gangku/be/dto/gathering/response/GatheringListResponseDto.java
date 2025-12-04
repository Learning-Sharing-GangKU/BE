package com.gangku.be.dto.gathering.response;

import com.gangku.be.model.gathering.GatheringList;
import com.gangku.be.model.gathering.GatheringListItem;
import com.gangku.be.model.common.PageMeta;
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
    private List<GatheringListItem> data;
    private PageMeta meta;

    public static GatheringListResponseDto from(GatheringList gatheringList) {
        return GatheringListResponseDto.builder()
                .data(gatheringList.data())
                .meta(gatheringList.meta())
                .build();
    }
}