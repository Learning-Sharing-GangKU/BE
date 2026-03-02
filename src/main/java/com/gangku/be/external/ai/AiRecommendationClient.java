package com.gangku.be.external.ai;

import com.gangku.be.dto.ai.AiRecommendRequestDto;
import java.util.List;

public interface AiRecommendationClient {
    List<Long> recommend(AiRecommendRequestDto request);
}