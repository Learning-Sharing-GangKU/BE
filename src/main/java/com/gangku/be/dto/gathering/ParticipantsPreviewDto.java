package com.gangku.be.dto.gathering;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

//모임 상세 조회시, 참여자 정렬을 위한 Dto
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantsPreviewDto {
    private List<ParticipantPreviewDto> data;
    private PageMetaDto meta;
}