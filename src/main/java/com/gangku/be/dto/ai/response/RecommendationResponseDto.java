package com.gangku.be.dto.ai.response;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RecommendationResponseDto {
    private List<Long> gatheringsId;
}
