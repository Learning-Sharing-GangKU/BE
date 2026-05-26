package com.gangku.be.service.gathering;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.gangku.be.dto.ai.request.TextFilterRequestDto;
import com.gangku.be.dto.ai.response.TextFilterResponseDto;
import com.gangku.be.dto.gathering.request.GatheringUpdateRequestDto;
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
public class UpdateGatheringUnitTest {

    @Mock private AiApiClient aiApiClient;
    @Mock private AiTextFilterMapper aiTextFilterMapper;
    @Mock private GatheringCommandService gatheringCommandService;

    @InjectMocks private GatheringService gatheringService;

    @Test
    @DisplayName("모임 수정 (200 OK): 금칙어 없으면 모임 수정 성공")
    void updateGathering_success() {
        // given
        Long gatheringId = 1L;
        Long userId = 10L;

        GatheringUpdateRequestDto requestDto =
                new GatheringUpdateRequestDto(
                        "제목 수정",
                        "statics/image/prod/2025/11/new.jpg",
                        "study",
                        15,
                        LocalDateTime.of(2025, 10, 5, 10, 0),
                        "공학관 302",
                        "https://open.kakao.com/o/xyz987",
                        "설명 업데이트");

        TextFilterRequestDto textFilterRequestDto = mock(TextFilterRequestDto.class);
        TextFilterResponseDto textFilterResponseDto = mock(TextFilterResponseDto.class);

        when(aiTextFilterMapper.fromGatheringUpdate(requestDto)).thenReturn(textFilterRequestDto);
        when(aiApiClient.filterText(textFilterRequestDto)).thenReturn(textFilterResponseDto);
        when(textFilterResponseDto.isAllowed()).thenReturn(true);
        when(gatheringCommandService.updateGathering(gatheringId, userId, requestDto))
                .thenReturn(mock(com.gangku.be.dto.gathering.response.GatheringResponseDto.class));

        // when
        gatheringService.updateGathering(gatheringId, userId, requestDto);

        // then
        verify(aiTextFilterMapper, times(1)).fromGatheringUpdate(requestDto);
        verify(aiApiClient, times(1)).filterText(textFilterRequestDto);
        verify(gatheringCommandService, times(1)).updateGathering(gatheringId, userId, requestDto);

        verifyNoMoreInteractions(aiTextFilterMapper, aiApiClient, gatheringCommandService);
    }

    @Test
    @DisplayName("모임 수정 (400 Bad Request): 금칙어 있으면 INVALID_GATHERING_CONTENT 예외")
    void updateGathering_invalidContent() {
        // given
        Long gatheringId = 1L;
        Long userId = 10L;

        GatheringUpdateRequestDto requestDto =
                new GatheringUpdateRequestDto(
                        "금칙어 제목",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "금칙어 설명");

        TextFilterRequestDto textFilterRequestDto = mock(TextFilterRequestDto.class);
        TextFilterResponseDto textFilterResponseDto = mock(TextFilterResponseDto.class);

        when(aiTextFilterMapper.fromGatheringUpdate(requestDto)).thenReturn(textFilterRequestDto);
        when(aiApiClient.filterText(textFilterRequestDto)).thenReturn(textFilterResponseDto);
        when(textFilterResponseDto.isAllowed()).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> gatheringService.updateGathering(gatheringId, userId, requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(GatheringErrorCode.INVALID_GATHERING_CONTENT);

        verify(gatheringCommandService, never()).updateGathering(any(), any(), any());

        verifyNoMoreInteractions(aiTextFilterMapper, aiApiClient);
        verifyNoInteractions(gatheringCommandService);
    }
}