package com.gangku.be.config.ai;

import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AiServerProps.class)
public class AiWebClientConfig {

    private final AiServerProps aiServerProps;

    @Bean
    public WebClient aiWebClient() {
        HttpClient httpClient =
                HttpClient.create()
                        .option(
                                ChannelOption.CONNECT_TIMEOUT_MILLIS,
                                aiServerProps.getConnectTimeoutMillis());
        //                        .responseTimeout(
        //
        // Duration.ofMillis(aiServerProps.getResponseTimeoutMillis()));

        return WebClient.builder()
                .baseUrl(aiServerProps.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
