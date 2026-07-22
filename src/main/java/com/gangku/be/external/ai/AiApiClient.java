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
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.model.ai.ClusteringRefreshResponse;
import com.gangku.be.model.ai.PopularityRefreshResponse;
import io.netty.handler.timeout.TimeoutException;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiApiClient {

    private final WebClient aiWebClient;
    private final AiServerProps aiServerProps;

    // --- 동기 래퍼: 호출 스레드에서 직접 실행 ---

    public IntroCreateResponseDto createIntro(IntroCreateRequestDto request) {
        return post(aiServerProps.getIntroPath(), request, IntroCreateResponseDto.class);
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

    // --- 비동기 진입점: aiTaskExecutor 스레드에서 실행 ---

    @Async("aiTaskExecutor")
    public CompletableFuture<IntroCreateResponseDto> createIntroAsync(
            IntroCreateRequestDto request) {
        return CompletableFuture.completedFuture(
                post(aiServerProps.getIntroPath(), request, IntroCreateResponseDto.class));
    }

    @Async("aiTaskExecutor")
    public CompletableFuture<TextFilterResponseDto> filterTextAsync(TextFilterRequestDto request) {
        return CompletableFuture.completedFuture(
                post(aiServerProps.getTextFilterPath(), request, TextFilterResponseDto.class));
    }

    @Async("aiTaskExecutor")
    public CompletableFuture<RecommendationResponseDto> recommendAsync(
            RecommendationRequestDto request) {
        return CompletableFuture.completedFuture(
                post(
                        aiServerProps.getRecommendationsPath(),
                        request,
                        RecommendationResponseDto.class));
    }

    @Async("aiTaskExecutor")
    public CompletableFuture<ClusteringRefreshResponse> refreshClusteringAsync(
            ClusteringRefreshRequestDto request) {
        return CompletableFuture.completedFuture(
                post(
                        aiServerProps.getRefreshClusteringPath(),
                        request,
                        ClusteringRefreshResponse.class));
    }

    @Async("aiTaskExecutor")
    public CompletableFuture<PopularityRefreshResponse> refreshPopularityAsync(
            PopularityRefreshRequestDto request) {
        return CompletableFuture.completedFuture(
                post(
                        aiServerProps.getRefreshPopularityPath(),
                        request,
                        PopularityRefreshResponse.class));
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
                            status -> status.value() == 400,
                            response ->
                                    response.bodyToMono(String.class)
                                            .defaultIfEmpty("")
                                            .map(
                                                    body -> {
                                                        log.warn(
                                                                "AI 서버 400 오류. uri={}, body={}",
                                                                uri,
                                                                body);
                                                        return new CustomException(
                                                                GatheringErrorCode
                                                                        .INVALID_GATHERING_CONTENT);
                                                    }))
                    .onStatus(
                            HttpStatusCode::is5xxServerError,
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
        } catch (TimeoutException e) {
            log.error("AI 서버 응답 시간 초과. uri={}, message={}", uri, e.getMessage(), e);
            throw new CustomException(CommonErrorCode.AI_TIMEOUT_ERROR);
        }
    }
}
