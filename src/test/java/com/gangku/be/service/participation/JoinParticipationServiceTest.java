package com.gangku.be.service.participation;

import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import com.gangku.be.dto.participation.ParticipationResponseDto;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JoinParticipationServiceTest {

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private GatheringRepository gatheringRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ParticipationService participationService;

    // =========================
    // 1. 정상 케이스
    // =========================

    @Test
    @DisplayName("존재하는 모임 + 유저, 아직 미참여 & 정원 여유 → 참여 성공")
    void joinParticipation_withValidIds_createsParticipationSuccessfully() {
        // given
        Long gatheringId = 1L;
        Long userId = 10L;

        Gathering gathering = mock(Gathering.class);
        User user = mock(User.class);

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByUserAndGathering(user, gathering)).thenReturn(false);
        when(gathering.getParticipantCount()).thenReturn(0);
        when(gathering.getCapacity()).thenReturn(10);

        when(participationRepository.save(any(Participation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ParticipationResponseDto response = participationService.joinParticipation(gatheringId, userId);

        // then
        assertNotNull(response, "응답 DTO는 null 이면 안 된다.");
        verify(participationRepository, times(1)).save(any(Participation.class));
        verify(gathering, times(1)).addParticipation(any(Participation.class));
    }

    @Test
    @DisplayName("첫 번째 참여자 → 참여자 수 0에서 정상 참여")
    void joinParticipation_whenFirstParticipant_joinsSuccessfully() {
        // given
        Long gatheringId = 2L;
        Long userId = 20L;

        Gathering gathering = mock(Gathering.class);
        User user = mock(User.class);

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByUserAndGathering(user, gathering)).thenReturn(false);
        when(gathering.getParticipantCount()).thenReturn(0);
        when(gathering.getCapacity()).thenReturn(5);

        when(participationRepository.save(any(Participation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ParticipationResponseDto response = participationService.joinParticipation(gatheringId, userId);

        // then
        assertNotNull(response);
        verify(participationRepository).save(any(Participation.class));
        verify(gathering).addParticipation(any(Participation.class));
    }

    @Test
    @DisplayName("정원 바로 직전(capacity-1) 상태 → 참여 허용")
    void joinParticipation_whenParticipantCountIsJustBelowCapacity_allowsJoin() {
        // given
        Long gatheringId = 3L;
        Long userId = 30L;

        Gathering gathering = mock(Gathering.class);
        User user = mock(User.class);

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByUserAndGathering(user, gathering)).thenReturn(false);
        when(gathering.getParticipantCount()).thenReturn(9);
        when(gathering.getCapacity()).thenReturn(10);

        when(participationRepository.save(any(Participation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ParticipationResponseDto response = participationService.joinParticipation(gatheringId, userId);

        // then
        assertNotNull(response);
        verify(participationRepository).save(any(Participation.class));
    }

    // =========================
    // 2. 예외 케이스
    // =========================

    @Test
    @DisplayName("존재하지 않는 모임 ID → GATHERING_NOT_FOUND")
    void joinParticipation_withNonExistingGathering_throwsGatheringNotFound() {
        // given
        Long gatheringId = 999L;
        Long userId = 10L;

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.empty());

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> participationService.joinParticipation(gatheringId, userId));

        // then
        assertEquals(GatheringErrorCode.GATHERING_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(userRepository, participationRepository);
    }

    @Test
    @DisplayName("존재하지 않는 유저 ID → USER_NOT_FOUND")
    void joinParticipation_withNonExistingUser_throwsUserNotFound() {
        // given
        Long gatheringId = 1L;
        Long userId = 999L;

        Gathering gathering = mock(Gathering.class);

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> participationService.joinParticipation(gatheringId, userId));

        // then
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(participationRepository, never()).existsByUserAndGathering(any(), any());
        verify(participationRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 참여한 유저가 다시 참여 시도 → ALREADY_JOINED")
    void joinParticipation_whenAlreadyJoined_throwsAlreadyJoined() {
        // given
        Long gatheringId = 1L;
        Long userId = 10L;

        Gathering gathering = mock(Gathering.class);
        User user = mock(User.class);

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByUserAndGathering(user, gathering)).thenReturn(true);

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> participationService.joinParticipation(gatheringId, userId));

        // then
        assertEquals(ParticipationErrorCode.ALREADY_JOINED, ex.getErrorCode());
        verify(participationRepository, never()).save(any());
        verify(gathering, never()).addParticipation(any());
    }

    @Test
    @DisplayName("정원이 이미 가득 찬 모임 → CAPACITY_FULL")
    void joinParticipation_whenCapacityAlreadyFull_throwsCapacityFull() {
        // given
        Long gatheringId = 1L;
        Long userId = 10L;

        Gathering gathering = mock(Gathering.class);
        User user = mock(User.class);

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByUserAndGathering(user, gathering)).thenReturn(false);
        when(gathering.getParticipantCount()).thenReturn(10);
        when(gathering.getCapacity()).thenReturn(10);

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> participationService.joinParticipation(gatheringId, userId));

        // then
        assertEquals(ParticipationErrorCode.CAPACITY_FULL, ex.getErrorCode());
        verify(participationRepository, never()).save(any());
    }

    // =========================
    // 3. 경계 케이스
    // =========================

    @Test
    @DisplayName("participantCount == capacity-1 → 마지막 슬롯 참여 허용")
    void joinParticipation_whenParticipantCountEqualsCapacityMinusOne_allowsLastSlot() {
        // given
        Long gatheringId = 5L;
        Long userId = 50L;

        Gathering gathering = mock(Gathering.class);
        User user = mock(User.class);

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByUserAndGathering(user, gathering)).thenReturn(false);
        when(gathering.getParticipantCount()).thenReturn(4);
        when(gathering.getCapacity()).thenReturn(5);

        when(participationRepository.save(any(Participation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ParticipationResponseDto response = participationService.joinParticipation(gatheringId, userId);

        // then
        assertNotNull(response);
        verify(participationRepository).save(any(Participation.class));
    }

    @Test
    @DisplayName("participantCount == capacity → 정원 초과로 참여 불가")
    void joinParticipation_whenParticipantCountEqualsCapacity_treatedAsFull() {
        // given
        Long gatheringId = 6L;
        Long userId = 60L;

        Gathering gathering = mock(Gathering.class);
        User user = mock(User.class);

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByUserAndGathering(user, gathering)).thenReturn(false);
        when(gathering.getParticipantCount()).thenReturn(5);
        when(gathering.getCapacity()).thenReturn(5);

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> participationService.joinParticipation(gatheringId, userId));

        // then
        assertEquals(ParticipationErrorCode.CAPACITY_FULL, ex.getErrorCode());
    }

    @Test
    @DisplayName("capacity == 0 인 비정상 모임 → 항상 정원 초과로 처리")
    void joinParticipation_withCapacityZero_alwaysFull() {
        // given
        Long gatheringId = 7L;
        Long userId = 70L;

        Gathering gathering = mock(Gathering.class);
        User user = mock(User.class);

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.existsByUserAndGathering(user, gathering)).thenReturn(false);
        when(gathering.getParticipantCount()).thenReturn(0);
        when(gathering.getCapacity()).thenReturn(0);

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> participationService.joinParticipation(gatheringId, userId));

        // then
        assertEquals(ParticipationErrorCode.CAPACITY_FULL, ex.getErrorCode());
    }

    @Test
    @DisplayName("같은 입력으로 두 번 호출 → 첫 번째 성공, 두 번째는 ALREADY_JOINED")
    void joinParticipation_whenUserAndGatheringAreSameAsPreviousCall_noResidualSideEffects() {
        // given
        Long gatheringId = 8L;
        Long userId = 80L;

        Gathering gathering = mock(Gathering.class);
        User user = mock(User.class);

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // 첫 호출: 아직 참여 안 함 → false
        // 두 번째 호출: 이미 참여한 상태라고 가정 → true
        when(participationRepository.existsByUserAndGathering(user, gathering))
                .thenReturn(false, true);

        when(gathering.getParticipantCount()).thenReturn(0);
        when(gathering.getCapacity()).thenReturn(10);
        when(participationRepository.save(any(Participation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when 1: 첫 번째 호출 → 정상 참여
        ParticipationResponseDto first = participationService.joinParticipation(gatheringId, userId);
        assertNotNull(first);

        // when 2: 두 번째 호출 → 이미 참여 예외
        CustomException ex = assertThrows(CustomException.class,
                () -> participationService.joinParticipation(gatheringId, userId));

        // then
        assertEquals(ParticipationErrorCode.ALREADY_JOINED, ex.getErrorCode());
        verify(participationRepository, times(1)).save(any(Participation.class));
    }
}
