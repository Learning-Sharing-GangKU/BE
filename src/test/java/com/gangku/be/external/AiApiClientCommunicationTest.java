package com.gangku.be.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gangku.be.config.ai.AiServerProps;
import com.gangku.be.dto.ai.request.TextFilterRequestDto;
import com.gangku.be.dto.ai.response.TextFilterResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.CommonErrorCode;
import com.gangku.be.external.ai.AiApiClient;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Tag("unit")
class AiApiClientCommunicationTest {

    private MockWebServer mockWebServer;
    private AiApiClient aiApiClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        objectMapper = new ObjectMapper();

        AiServerProps aiServerProps = new AiServerProps();
        aiServerProps.setBaseUrl(mockWebServer.url("/").toString());
        aiServerProps.setConnectTimeoutMillis(3000);
        aiServerProps.setResponseTimeoutMillis(3000);
        aiServerProps.setTextFilterPath("/api/ai/filter-text");
        aiServerProps.setIntroPath("/api/ai/intro");
        aiServerProps.setRecommendationsPath("/api/ai/recommend");
        aiServerProps.setRefreshClusteringPath("/api/ai/clustering/refresh");
        aiServerProps.setRefreshPopularityPath("/api/ai/popularity/refresh");

        WebClient webClient =
                WebClient.builder()
                        .baseUrl(aiServerProps.getBaseUrl())
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build();

        aiApiClient = new AiApiClient(webClient, aiServerProps);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("AI 통신 성공: filterText 호출 시 200 응답을 TextFilterResponseDto로 반환")
    void filterText_success() throws Exception {
        // given
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(
                                """
                                        {
                                          "allowed": true
                                        }
                                        """));

        TextFilterRequestDto requestDto = new TextFilterRequestDto("테스트");

        // when
        TextFilterResponseDto response = aiApiClient.filterText(requestDto);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isAllowed()).isTrue();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/ai/filter-text");

        String requestBody = recordedRequest.getBody().readUtf8();
        assertThat(requestBody).contains("테스트");
    }

    @Test
    @DisplayName("AI 통신 성공: filterText 호출 시 allowed=false 응답도 정상 매핑")
    void filterText_success_blocked() {
        // given
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(
                                """
                                        {
                                          "allowed": false
                                        }
                                        """));

        TextFilterRequestDto requestDto = new TextFilterRequestDto("금칙어");

        // when
        TextFilterResponseDto response = aiApiClient.filterText(requestDto);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isAllowed()).isFalse();
    }

    @Test
    @DisplayName("AI 통신 실패: 500 응답이면 AI_SERVICE_ERROR 예외")
    void filterText_serverError() {
        // given
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(500)
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .setBody(
                                """
                                        {
                                          "error": "internal server error"
                                        }
                                        """));

        TextFilterRequestDto requestDto = new TextFilterRequestDto("테스트");

        // when & then
        assertThatThrownBy(() -> aiApiClient.filterText(requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.AI_SERVICE_ERROR);
    }
}
