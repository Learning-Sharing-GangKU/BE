package com.gangku.be.external.ai.client;

import com.gangku.be.config.ai.AiServerProps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiTextFilterClient {

    private final WebClient aiWebClient;
    private final AiServerProps aiServerProps;

    public String filterTextV2(Object request) {
        String uri = aiServerProps.getTextFilter().getV2();


    }

}
