package com.gangku.be.external.ai;

import com.gangku.be.config.ai.AiServerProps;
import com.gangku.be.dto.ai.request.ClusteringRefreshRequestDto;
import com.gangku.be.dto.ai.request.IntroCreateRequestDto;
import com.gangku.be.dto.ai.request.PopularityRefreshRequestDto;
import com.gangku.be.dto.ai.request.RecommendationRequestDto;
import com.gangku.be.dto.ai.request.TextFilterRequestDto;
import com.gangku.be.dto.ai.response.IntroCreateResponseDto;
import com.gangku.be.dto.ai.response.RecommendationResponseDto;
import com.gangku.be.dto.ai.response.TextFilterResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.CommonErrorCode;
import com.gangku.be.model.ai.ClusteringRefreshResponse;
import com.gangku.be.model.ai.PopularityRefreshResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                aiServerProps.getIntroPath(), introCreateRequestDto, IntroCreateResponseDto.class);
    }

    public TextFilterResponseDto filterText(TextFilterRequestDto request) {
        return post(aiServerProps.getTextFilterPath(), request, TextFilterResponseDto.class);
    }

    public RecommendationResponseDto recommend(RecommendationRequestDto request) {
        return post(
                aiServerProps.getRecommendationsPath(), request, RecommendationResponseDto.class);
    }

    public ClusteringRefreshResponse refreshClustering(ClusteringRefreshRequestDto request) {
        return post(
                aiServerProps.getRefreshClusteringPath(), request, ClusteringRefreshResponse.class);
    }

    public PopularityRefreshResponse refreshPopularity(PopularityRefreshRequestDto request) {
        return post(
                aiServerProps.getRefreshPopularityPath(), request, PopularityRefreshResponse.class);
    }

    private <T> T post(String uri, Object requestDto, Class<T> responseType) {
        try {
            return aiWebClient
                    .post()
                    .uri(uri)
                    .bodyValue(requestDto)
                    .retrieve()
                    .onStatus(
                            status -> status.value() == 422,
                            response ->
                                    response.bodyToMono(String.class)
                                            .defaultIfEmpty("")
                                            .map(
                                                    body -> {
                                                        log.warn(
                                                                "AI 서버 422 오류. uri={}, body={}",
                                                                uri,
                                                                body);
                                                        return new CustomException(
                                                                CommonErrorCode
                                                                        .AI_VALIDATION_ERROR);
                                                    }))
                    .onStatus(
                            status -> status.is5xxServerError(),
                            response ->
                                    response.bodyToMono(String.class)
                                            .defaultIfEmpty("")
                                            .map(
                                                    body -> {
                                                        log.error(
                                                                "AI 서버 5xx 오류. uri={}, body={}",
                                                                uri,
                                                                body);
                                                        return new CustomException(
                                                                CommonErrorCode.AI_SERVICE_ERROR);
                                                    }))
                    .bodyToMono(responseType)
                    .block();

        } catch (WebClientException e) {
            log.error("AI 서버 통신 실패. uri={}, message={}", uri, e.getMessage(), e);
            throw new CustomException(CommonErrorCode.AI_SERVICE_ERROR);
        }
    }
}
