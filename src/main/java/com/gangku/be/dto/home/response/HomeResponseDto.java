package com.gangku.be.dto.home.response;

import com.gangku.be.dto.gathering.response.GatheringListResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HomeResponseDto {

    private GatheringListResponseDto recommended;
    private GatheringListResponseDto latest;
    private GatheringListResponseDto popular;
}
