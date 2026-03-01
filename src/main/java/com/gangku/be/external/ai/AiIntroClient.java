package com.gangku.be.external.ai;

import com.gangku.be.dto.gathering.request.GatheringIntroRequestDto;
import com.gangku.be.dto.gathering.response.GatheringIntroResponseDto;

public interface AiIntroClient {
    GatheringIntroResponseDto createIntro(GatheringIntroRequestDto request);
}