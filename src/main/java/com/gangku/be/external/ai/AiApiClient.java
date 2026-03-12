package com.gangku.be.external.ai;

import com.gangku.be.config.ai.AiServerProps;
import com.gangku.be.dto.ai.request.ClusteringRefreshRequestDto;
import com.gangku.be.dto.ai.request.IntroCreateRequestDto;
import com.gangku.be.dto.ai.request.PopularityRefreshRequestDto;
import com.gangku.be.dto.ai.request.RecommendationRequestDto;
import com.gangku.be.dto.ai.request.TextFilterRequestDto;
import com.gangku.be.dto.ai.response.ClusteringRefreshResponseDto;
import com.gangku.be.dto.ai.response.IntroCreateResponseDto;
import com.gangku.be.dto.ai.response.PopularityRefreshResponseDto;
import com.gangku.be.dto.ai.response.RecommendationResponseDto;
import com.gangku.be.dto.ai.response.TextFilterResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.DecodingException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiApiClient {

    private final WebClient aiWebClient;
    private final AiServerProps aiServerProps;

    public IntroCreateResponseDto createIntro(IntroCreateRequestDto introCreateRequestDto) {
        return post(
                aiServerProps.getIntroPath(),
                introCreateRequestDto,
                IntroCreateResponseDto.class
        );
    }

    public TextFilterResponseDto filterText(TextFilterRequestDto request) {
        return post(
                aiServerProps.getTextFilterPath(),
                request,
                TextFilterResponseDto.class
        );
    }

    public RecommendationResponseDto recommend(RecommendationRequestDto request) {
        return post(
                aiServerProps.getRecommendationsPath(),
                request,
                RecommendationResponseDto.class
        );
    }

    public ClusteringRefreshResponseDto refreshClustering(ClusteringRefreshRequestDto request) {
        return post(
                aiServerProps.getRefreshClusteringPath(),
                request,
                ClusteringRefreshResponseDto.class
        );
    }

    public PopularityRefreshResponseDto refreshPopularity(PopularityRefreshRequestDto request) {
        return post(
                aiServerProps.getRefreshPopularityPath(),
                request,
                PopularityRefreshResponseDto.class
        );
    }

    private <T> T post(String uri, Object requestDto, Class<T> responseType) {
        try {
            return aiWebClient.post()
                    .uri(uri)
                    .bodyValue(requestDto)
                    .retrieve()
                    .bodyToMono(responseType)
                    .block();
        } catch (WebClientException | DecodingException e) {
            log.error("AI 서버 통신 실패. uri={}, message={}", uri, e.getMessage(), e);
            throw new CustomException(CommonErrorCode.AI_SERVICE_ERROR);
        }
    }
}
