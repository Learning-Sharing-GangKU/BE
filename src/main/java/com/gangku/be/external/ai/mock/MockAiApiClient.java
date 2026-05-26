package com.gangku.be.external.ai.mock;

import com.gangku.be.config.ai.AiServerProps;
import com.gangku.be.dto.ai.request.*;
import com.gangku.be.dto.ai.response.*;
import com.gangku.be.external.ai.AiApiClient;
import com.gangku.be.model.ai.*;
import java.lang.reflect.Field;
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
    public TextFilterResponseDto filterText(TextFilterRequestDto request) {
        delay();

        try {
            TextFilterResponseDto response = new TextFilterResponseDto();
            Field field = TextFilterResponseDto.class.getDeclaredField("allowed");
            field.setAccessible(true);
            field.set(response, true);
            return response;
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