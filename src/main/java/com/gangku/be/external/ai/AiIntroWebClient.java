package com.gangku.be.external.ai;

import com.gangku.be.dto.gathering.request.GatheringIntroRequestDto;
import com.gangku.be.dto.gathering.response.GatheringIntroResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.GatheringErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AiIntroWebClient implements AiIntroClient {

    private final WebClient webClient;

    @Value("${ai.server.base-url}")
    private String aiServerBaseUrl;

    @Override
    public GatheringIntroResponseDto createIntro(GatheringIntroRequestDto request) {
        return webClient.post()
                .uri(aiServerBaseUrl + "/ai/v1/gatherings/intro")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        r -> Mono.error(new CustomException(GatheringErrorCode.AI_SERVICE_UNAVAILABLE))
                )
                .bodyToMono(GatheringIntroResponseDto.class)
                .block();
    }
}