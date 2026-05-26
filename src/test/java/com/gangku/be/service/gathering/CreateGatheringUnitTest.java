package com.gangku.be.service.gathering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.gangku.be.dto.ai.request.TextFilterRequestDto;
import com.gangku.be.dto.ai.response.TextFilterResponseDto;
import com.gangku.be.dto.gathering.request.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.response.GatheringResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.external.ai.AiApiClient;
import com.gangku.be.service.GatheringService;
import com.gangku.be.service.command.GatheringCommandService;
import com.gangku.be.util.ai.AiTextFilterMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class CreateGatheringUnitTest {

    @Mock private AiApiClient aiApiClient;
    @Mock private AiTextFilterMapper aiTextFilterMapper;
    @Mock private GatheringCommandService gatheringCommandService;

    @InjectMocks private GatheringService gatheringService;

    @Test
    @DisplayName("모임 생성 (201 Created): 금칙어 없으면 모임 생성 성공")
    void createGathering_success() {
        // given
        Long hostId = 1L;
        GatheringCreateRequestDto requestDto =
                new GatheringCreateRequestDto(
                        "알고리즘 스터디",
                        "statics/image/prod/2025/11/efe6-a7d.jpg",
                        "study",
                        12,
                        LocalDateTime.of(2025, 10, 1, 10, 0),
                        "공학관 301",
                        "https://open.kakao.com/o/abcdef",
                        "기초부터 차근차근 알고리즘을 공부합니다.");

        TextFilterRequestDto textFilterRequestDto = mock(TextFilterRequestDto.class);
        TextFilterResponseDto textFilterResponseDto = mock(TextFilterResponseDto.class);
        GatheringResponseDto expectedResponse = mock(GatheringResponseDto.class);

        when(aiTextFilterMapper.fromGatheringCreate(requestDto)).thenReturn(textFilterRequestDto);
        when(aiApiClient.filterText(textFilterRequestDto)).thenReturn(textFilterResponseDto);
        when(textFilterResponseDto.isAllowed()).thenReturn(true);
        when(gatheringCommandService.saveGathering(requestDto, hostId)).thenReturn(expectedResponse);

        // when
        GatheringResponseDto response = gatheringService.createGathering(requestDto, hostId);

        // then
        assertThat(response).isEqualTo(expectedResponse);

        verify(aiTextFilterMapper, times(1)).fromGatheringCreate(requestDto);
        verify(aiApiClient, times(1)).filterText(textFilterRequestDto);
        verify(gatheringCommandService, times(1)).saveGathering(requestDto, hostId);

        verifyNoMoreInteractions(aiTextFilterMapper, aiApiClient, gatheringCommandService);
    }

    @Test
    @DisplayName("모임 생성 (400 Bad Request): 금칙어 있으면 INVALID_GATHERING_CONTENT 예외")
    void createGathering_invalidContent() {
        // given
        Long hostId = 1L;
        GatheringCreateRequestDto requestDto =
                new GatheringCreateRequestDto(
                        "부적절한 모임 제목",
                        "statics/image/prod/2025/11/efe6-a7d.jpg",
                        "study",
                        12,
                        LocalDateTime.of(2025, 10, 1, 10, 0),
                        "공학관 301",
                        "https://open.kakao.com/o/abcdef",
                        "부적절한 설명");

        TextFilterRequestDto textFilterRequestDto = mock(TextFilterRequestDto.class);
        TextFilterResponseDto textFilterResponseDto = mock(TextFilterResponseDto.class);

        when(aiTextFilterMapper.fromGatheringCreate(requestDto)).thenReturn(textFilterRequestDto);
        when(aiApiClient.filterText(textFilterRequestDto)).thenReturn(textFilterResponseDto);
        when(textFilterResponseDto.isAllowed()).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> gatheringService.createGathering(requestDto, hostId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(GatheringErrorCode.INVALID_GATHERING_CONTENT);

        verify(gatheringCommandService, never()).saveGathering(any(), any());

        verifyNoMoreInteractions(aiTextFilterMapper, aiApiClient);
        verifyNoInteractions(gatheringCommandService);
    }
}