package com.gangku.be.service.participation;

import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.exception.constant.ParticipationErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.ParticipationService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelParticipationServiceTest {

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private GatheringRepository gatheringRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ParticipationService participationService;

    // =========================================================
    // 1. 정상 케이스
    // =========================================================

    @Test
    @DisplayName("유효한 gatheringId, userId → 참여 취소 & delete 호출")
    void cancelParticipation_withValidIds_deletesParticipationSuccessfully() {
        // given
        Long gatheringId = 1L;
        Long userId = 10L;

        Gathering gathering = mock(Gathering.class);
        User user = mock(User.class);
        Participation participation = mock(Participation.class);
        User host = mock(User.class);

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.findByUserAndGathering(user, gathering))
                .thenReturn(Optional.of(participation));

        // host != user 인 상황 (일반 참여자)
        when(gathering.getHost()).thenReturn(host);
        when(host.getId()).thenReturn(999L);
        when(user.getId()).thenReturn(userId);

        // when
        participationService.cancelParticipation(gatheringId, userId);

        // then
        verify(gathering, times(1)).removeParticipation(participation);
        verify(participationRepository, times(1)).delete(participation);
    }

    // =========================================================
    // 2. 예외 케이스
    // =========================================================

    @Test
    @DisplayName("존재하지 않는 모임 ID → GATHERING_NOT_FOUND")
    void cancelParticipation_withNonExistingGathering_throwsGatheringNotFound() {
        // given
        Long gatheringId = 999L;
        Long userId = 10L;

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.empty());

        // when
        CustomException ex = assertThrows(
                CustomException.class,
                () -> participationService.cancelParticipation(gatheringId, userId)
        );

        // then
        assertEquals(GatheringErrorCode.GATHERING_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(userRepository, participationRepository);
    }

    @Test
    @DisplayName("존재하지 않는 유저 ID → USER_NOT_FOUND")
    void cancelParticipation_withNonExistingUser_throwsUserNotFound() {
        // given
        Long gatheringId = 1L;
        Long userId = 999L;

        Gathering gathering = mock(Gathering.class);
        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when
        CustomException ex = assertThrows(
                CustomException.class,
                () -> participationService.cancelParticipation(gatheringId, userId)
        );

        // then
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(participationRepository);
    }

    @Test
    @DisplayName("이미 탈퇴한 사용자(Participation 없음) → ALREADY_LEFT")
    void cancelParticipation_whenParticipationNotFound_throwsAlreadyLeft() {
        // given
        Long gatheringId = 1L;
        Long userId = 10L;

        Gathering gathering = mock(Gathering.class);
        User user = mock(User.class);

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(participationRepository.findByUserAndGathering(user, gathering))
                .thenReturn(Optional.empty()); // 이미 나간 상태

        // when
        CustomException ex = assertThrows(
                CustomException.class,
                () -> participationService.cancelParticipation(gatheringId, userId)
        );

        // then
        assertEquals(ParticipationErrorCode.ALREADY_LEFT, ex.getErrorCode());
        verify(gathering, never()).removeParticipation(any());
        verify(participationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("호스트가 탈퇴 시도 → HOST_CANNOT_LEAVE")
    void cancelParticipation_whenHostTriesToLeave_throwsHostCannotLeave() {
        // given
        Long gatheringId = 1L;
        Long hostId = 10L;

        Gathering gathering = mock(Gathering.class);
        User host = mock(User.class);
        Participation participation = mock(Participation.class);

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(userRepository.findById(hostId)).thenReturn(Optional.of(host));

        when(participationRepository.findByUserAndGathering(host, gathering))
                .thenReturn(Optional.of(participation));

        // host == user
        when(gathering.getHost()).thenReturn(host);
        when(host.getId()).thenReturn(hostId);

        // when
        CustomException ex = assertThrows(
                CustomException.class,
                () -> participationService.cancelParticipation(gatheringId, hostId)
        );

        // then
        assertEquals(ParticipationErrorCode.HOST_CANNOT_LEAVE, ex.getErrorCode());
        verify(gathering, never()).removeParticipation(any());
        verify(participationRepository, never()).delete(any());
    }

    // =========================================================
    // 3. 경계 / 시나리오 일관성 케이스
    // =========================================================

    @Test
    @DisplayName("같은 유저가 두 번 연속 취소 시도 → 1회차 성공, 2회차 ALREADY_LEFT")
    void cancelParticipation_whenCalledTwice_secondCallThrowsAlreadyLeft() {
        // given
        Long gatheringId = 1L;
        Long userId = 10L;

        Gathering gathering = mock(Gathering.class);
        User user = mock(User.class);
        Participation participation = mock(Participation.class);
        User host = mock(User.class);

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // host != user
        when(gathering.getHost()).thenReturn(host);
        when(host.getId()).thenReturn(999L);
        when(user.getId()).thenReturn(userId);

        // 첫 번째 호출: 존재, 두 번째 호출: 존재하지 않음
        when(participationRepository.findByUserAndGathering(user, gathering))
                .thenReturn(Optional.of(participation))   // 1st
                .thenReturn(Optional.empty());           // 2nd

        // 1) first call: success
        assertDoesNotThrow(() ->
                participationService.cancelParticipation(gatheringId, userId)
        );

        // then: delete 1회 호출
        verify(participationRepository, times(1)).delete(participation);
        verify(gathering, times(1)).removeParticipation(participation);

        // 2) second call: already left
        CustomException ex = assertThrows(
                CustomException.class,
                () -> participationService.cancelParticipation(gatheringId, userId)
        );
        assertEquals(ParticipationErrorCode.ALREADY_LEFT, ex.getErrorCode());

        // 삭제는 여전히 1회만 호출
        verify(participationRepository, times(1)).delete(participation);
    }

    @Test
    @DisplayName("참여자가 1명뿐인 모임에서 취소 → 정상 삭제 (마지막 참여자)")
    void cancelParticipation_whenLastParticipant_leavesSuccessfully() {
        // given
        Long gatheringId = 1L;
        Long userId = 10L;

        Gathering gathering = mock(Gathering.class);
        User user = mock(User.class);
        Participation participation = mock(Participation.class);
        User host = mock(User.class);

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(participationRepository.findByUserAndGathering(user, gathering))
                .thenReturn(Optional.of(participation));

        // host != user (일반 참가자)
        when(gathering.getHost()).thenReturn(host);
        when(host.getId()).thenReturn(999L);
        when(user.getId()).thenReturn(userId);

        // when
        assertDoesNotThrow(() ->
                participationService.cancelParticipation(gatheringId, userId)
        );

        // then
        verify(gathering, times(1)).removeParticipation(participation);
        verify(participationRepository, times(1)).delete(participation);
    }
}
