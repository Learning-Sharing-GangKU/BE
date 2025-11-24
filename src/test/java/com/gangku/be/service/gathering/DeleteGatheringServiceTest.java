package com.gangku.be.service.gathering;

import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.User;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.GatheringService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeleteGatheringServiceTest {

    @Mock
    private GatheringRepository gatheringRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GatheringService gatheringService;

    private Gathering createGatheringWithHost(Long hostId) {
        User host = new User();
        host.setId(hostId);

        Gathering gathering = new Gathering();
        gathering.setHost(host);

        return gathering;
    }

    // =========================================================
    // 1. 정상 케이스
    // =========================================================

    @Test
    @DisplayName("모임 호스트가 삭제 요청 → 정상 삭제 & delete 1회 호출")
    void deleteGathering_withHostUser_deletesSuccessfully() {
        // given
        Long gatheringId = 1L;
        Long hostId = 10L;

        Gathering gathering = createGatheringWithHost(hostId);

        when(gatheringRepository.findById(gatheringId))
                .thenReturn(Optional.of(gathering));

        // when & then (예외 없어야 함)
        assertDoesNotThrow(() ->
                gatheringService.deleteGathering(gatheringId, hostId)
        );

        verify(gatheringRepository, times(1)).delete(gathering);
    }

    @Test
    @DisplayName("delete 호출 시 findById로 로딩한 동일 엔티티가 사용되는지 검증")
    void deleteGathering_withHostUser_deleteCalledWithLoadedEntity() {
        // given
        Long gatheringId = 1L;
        Long hostId = 10L;

        Gathering gathering = createGatheringWithHost(hostId);

        when(gatheringRepository.findById(gatheringId))
                .thenReturn(Optional.of(gathering));

        // when
        gatheringService.deleteGathering(gatheringId, hostId);

        // then
        // delete가 findById에서 반환한 동일 인스턴스로 호출되었는지 검증
        verify(gatheringRepository, times(1)).delete(same(gathering));
    }

    // =========================================================
    // 2. 예외 케이스
    // =========================================================

    @Test
    @DisplayName("존재하지 않는 모임 ID → GATHERING_NOT_FOUND")
    void deleteGathering_withNonExistingGathering_throwsGatheringNotFound() {
        // given
        Long gatheringId = 999L;
        Long userId = 10L;

        when(gatheringRepository.findById(gatheringId))
                .thenReturn(Optional.empty());

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> gatheringService.deleteGathering(gatheringId, userId));

        // then
        assertEquals(GatheringErrorCode.GATHERING_NOT_FOUND, ex.getErrorCode());
        verify(gatheringRepository, never()).delete(any(Gathering.class));
    }

    @Test
    @DisplayName("호스트가 아닌 유저가 삭제 요청 → FORBIDDEN")
    void deleteGathering_withNonHostUser_throwsForbidden() {
        // given
        Long gatheringId = 1L;
        Long hostId = 10L;      // 실제 호스트
        Long otherUserId = 20L; // 삭제 요청 유저

        Gathering gathering = createGatheringWithHost(hostId);

        when(gatheringRepository.findById(gatheringId))
                .thenReturn(Optional.of(gathering));

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> gatheringService.deleteGathering(gatheringId, otherUserId));

        // then
        assertEquals(GatheringErrorCode.FORBIDDEN, ex.getErrorCode());
        verify(gatheringRepository, never()).delete(any(Gathering.class));
    }

    @Test
    @DisplayName("delete() 수행 중 Repository에서 예외 발생 → 예외 그대로 전파")
    void deleteGathering_whenRepositoryDeleteThrowsException_propagatesException() {
        // given
        Long gatheringId = 1L;
        Long hostId = 10L;

        Gathering gathering = createGatheringWithHost(hostId);

        when(gatheringRepository.findById(gatheringId))
                .thenReturn(Optional.of(gathering));

        // delete 호출 시 런타임 예외 발생하도록 설정
        doThrow(new RuntimeException("DB error"))
                .when(gatheringRepository).delete(gathering);

        // when & then
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> gatheringService.deleteGathering(gatheringId, hostId));

        assertEquals("DB error", ex.getMessage());
    }

    // =========================================================
    // 3. 경계 케이스
    // =========================================================

    @Test
    @DisplayName("매우 큰 gatheringId(Long.MAX_VALUE) → GATHERING_NOT_FOUND 처리")
    void deleteGathering_withVeryLargeGatheringIdNotFound_throwsGatheringNotFound() {
        // given
        Long gatheringId = Long.MAX_VALUE;
        Long userId = 10L;

        when(gatheringRepository.findById(gatheringId))
                .thenReturn(Optional.empty());

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> gatheringService.deleteGathering(gatheringId, userId));

        // then
        assertEquals(GatheringErrorCode.GATHERING_NOT_FOUND, ex.getErrorCode());
        verify(gatheringRepository, never()).delete(any(Gathering.class));
    }

    @Test
    @DisplayName("gatheringId가 0 또는 음수인 경우 → 내부적으로 NOT_FOUND 취급")
    void deleteGathering_withZeroOrNegativeGatheringId_treatedAsNotFound() {
        Long userId = 10L;

        // case 1: 0L
        Long zeroId = 0L;
        when(gatheringRepository.findById(zeroId))
                .thenReturn(Optional.empty());

        CustomException exZero = assertThrows(CustomException.class,
                () -> gatheringService.deleteGathering(zeroId, userId));
        assertEquals(GatheringErrorCode.GATHERING_NOT_FOUND, exZero.getErrorCode());

        // case 2: -1L
        Long negativeId = -1L;
        when(gatheringRepository.findById(negativeId))
                .thenReturn(Optional.empty());

        CustomException exNegative = assertThrows(CustomException.class,
                () -> gatheringService.deleteGathering(negativeId, userId));
        assertEquals(GatheringErrorCode.GATHERING_NOT_FOUND, exNegative.getErrorCode());

        verify(gatheringRepository, never()).delete(any(Gathering.class));
    }

    @Test
    @DisplayName("호스트 ID가 Long.MAX_VALUE인 경우에도 정상 삭제")
    void deleteGathering_withHostIdAtLongMaxValue_deletesSuccessfully() {
        // given
        Long gatheringId = 1L;
        Long hostId = Long.MAX_VALUE;

        Gathering gathering = createGatheringWithHost(hostId);

        when(gatheringRepository.findById(gatheringId))
                .thenReturn(Optional.of(gathering));

        // when & then
        assertDoesNotThrow(() ->
                gatheringService.deleteGathering(gatheringId, hostId)
        );

        verify(gatheringRepository, times(1)).delete(gathering);
    }
}
