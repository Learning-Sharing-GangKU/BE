package com.gangku.be.external.ai;

import com.gangku.be.dto.ai.request.AiRecommendRequestDto;
import com.gangku.be.dto.ai.response.AiRecommendResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.CommonErrorCode;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AiRecommendationWebClient implements AiRecommendationClient {

    private final WebClient webClient;

    @Value("${ai.server.base-url}")
    private String aiServerBaseUrl;

    @Override
    public List<Long> recommend(AiRecommendRequestDto request) {
        AiRecommendResponseDto response =
                webClient
                        .post()
                        .uri(aiServerBaseUrl + "/api/ai/v1/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .retrieve()
                        .onStatus(
                                HttpStatusCode::is5xxServerError,
                                r ->
                                        Mono.error(
                                                new CustomException(
                                                        CommonErrorCode.AI_SERVICE_ERROR)))
                        .bodyToMono(AiRecommendResponseDto.class)
                        .block();

            if (response == null || response.getItems() == null) {
                return Collections.emptyList();
            }
            return response.getItems();
        } catch (Exception e) {

            // 🔥 AI 서버 안 켜져있을 때 fallback
            return List.of(1L, 2L, 3L);
        }
    }
}
