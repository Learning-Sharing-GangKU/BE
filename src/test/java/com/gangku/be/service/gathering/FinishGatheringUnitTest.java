package com.gangku.be.service.gathering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.gangku.be.constant.gathering.GatheringStatus;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.User;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.service.GatheringService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class FinishGatheringUnitTest {

    @Mock private GatheringRepository gatheringRepository;

    @InjectMocks private GatheringService gatheringService;

    @Test
    @DisplayName("모임 종료 (204 No Content): 호스트가 요청하면 상태를 FINISHED로 변경")
    void finishGathering_success() {
        // given
        Long gatheringId = 1L;
        Long userId = 100L;

        User host = User.builder().id(userId).build();
        Gathering gathering =
                Gathering.builder().id(gatheringId).host(host).status(GatheringStatus.RECRUITING).build();

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));

        // when
        gatheringService.finishGathering(gatheringId, userId);

        // then
        assertThat(gathering.getStatus()).isEqualTo(GatheringStatus.FINISHED);
        verify(gatheringRepository, times(1)).findById(gatheringId);
        verify(gatheringRepository, times(1)).save(gathering);
    }

    @Test
    @DisplayName("모임 종료 (404 Not Found): 모임이 존재하지 않으면 GATHERING_NOT_FOUND 예외")
    void finishGathering_notFound() {
        // given
        Long gatheringId = 999L;
        Long userId = 100L;

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> gatheringService.finishGathering(gatheringId, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(GatheringErrorCode.GATHERING_NOT_FOUND);

        verify(gatheringRepository, times(1)).findById(gatheringId);
        verify(gatheringRepository, never()).save(any());
    }

    @Test
    @DisplayName("모임 종료 (403 Forbidden): 호스트가 아니면 NO_PERMISSION_TO_MANIPULATE_GATHERING 예외")
    void finishGathering_noPermission() {
        // given
        Long gatheringId = 1L;
        Long hostId = 100L;
        Long otherUserId = 200L;

        User host = User.builder().id(hostId).build();
        Gathering gathering =
                Gathering.builder().id(gatheringId).host(host).status(GatheringStatus.RECRUITING).build();

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));

        // when & then
        assertThatThrownBy(() -> gatheringService.finishGathering(gatheringId, otherUserId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(GatheringErrorCode.NO_PERMISSION_TO_MANIPULATE_GATHERING);

        verify(gatheringRepository, times(1)).findById(gatheringId);
        verify(gatheringRepository, never()).save(any());
        assertThat(gathering.getStatus()).isNotEqualTo(GatheringStatus.FINISHED);
    }
}
