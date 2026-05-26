package com.gangku.be.external.ai.mock;

import com.gangku.be.config.ai.AiServerProps;
import com.gangku.be.dto.ai.request.TextFilterRequestDto;
import com.gangku.be.dto.ai.response.TextFilterResponseDto;
import com.gangku.be.external.ai.AiApiClient;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@Primary
@Profile("local")
public class MockAiApiClient extends AiApiClient {

    public MockAiApiClient(WebClient aiWebClient, AiServerProps aiServerProps) {
        super(aiWebClient, aiServerProps);
    }

    @Override
    public CompletableFuture<TextFilterResponseDto> filterTextAsync(
            TextFilterRequestDto request) {
        delay();

        try {
            TextFilterResponseDto response = new TextFilterResponseDto();
            Field field = TextFilterResponseDto.class.getDeclaredField("allowed");
            field.setAccessible(true);
            field.set(response, true);
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            throw new RuntimeException("Mock 생성 실패", e);
        }
    }

    private void delay() {
        try {
            log.info("[MockAiApiClient] 지연 시작 | thread={}", Thread.currentThread().getName());
            Thread.sleep(30_000);
            log.info("[MockAiApiClient] 지연 완료 | thread={}", Thread.currentThread().getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}