package com.gangku.be.service;

import com.gangku.be.domain.*;
import com.gangku.be.dto.gathering.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.GatheringCreateResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.ErrorCode;
import com.gangku.be.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class ParticipationServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private GatheringRepository gatheringRepository;

    @Autowired
    private GatheringService gatheringService;

    @Autowired
    private ParticipationService participationService;

    @Autowired
    private ParticipationRepository participationRepository;

    private User guestUser;
    private User hostUser;
    private Gathering savedGathering;

    @BeforeEach
    void setup() {
        // 사용자 2명 생성
        hostUser = userRepository.save(User.builder()
                .email("host@gangku.com")
                .nickname("호스트")
                .password("password")
                .photoUrl("https://photo.url/host")
                .build());

        guestUser = userRepository.save(User.builder()
                .email("guest@gangku.com")
                .nickname("게스트")
                .password("password")
                .photoUrl("https://photo.url/guest")
                .build());

        // 카테고리 저장
        Category category = categoryRepository.findByName("스터디").orElseGet(() ->
                categoryRepository.save(Category.builder().name("스터디").build()));

        // 모임 생성
        GatheringCreateRequestDto request = new GatheringCreateRequestDto(
                "참여 테스트 모임",
                "https://img.url",
                "스터디",
                5,
                LocalDateTime.of(2025, 11, 10, 18, 0),
                "온라인 Zoom",
                "https://open.kakao.com/join-room",
                "참여 테스트용 모임입니다."
        );
        GatheringCreateResponseDto response = gatheringService.createGathering(request, hostUser);
        savedGathering = gatheringRepository.findById(
                Long.parseLong(response.getId().replace("gath_", ""))
        ).orElseThrow(() -> new IllegalArgumentException("모임 생성 실패"));
    }

    @Test
    @DisplayName("유저가 모임에 정상적으로 참여해야 한다")
    void joinGathering_정상참여() {
        // when
        participationService.join(savedGathering.getId(), guestUser);

        // then
        boolean exists = participationRepository.existsByUserAndGathering(guestUser, savedGathering);
        assertThat(exists).isTrue();

        Gathering updated = gatheringService.getGatheringById(savedGathering.getId());
        assertThat(updated.getParticipantCount()).isEqualTo(2); // 호스트 1 + 게스트 1
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 모임 참여 시 USER_NOT_FOUND 예외 발생")
    void joinGathering_존재하지않는유저_예외() {
        // given
        // 실제 DB에는 존재하지 않는 user 객체 생성 (id만 수동 세팅)
        User invalidUser = new User();
        invalidUser.setId(99999L); // DB에 없는 ID

        // when & then
        CustomException ex = assertThrows(CustomException.class, () ->
                participationService.join(savedGathering.getId(), invalidUser));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(ex.getMessage()).contains("존재하지 않는 사용자입니다");
    }

    @Test
    @DisplayName("존재하지 않는 모임에 참여할 경우 GATHERING_NOT_FOUND 예외 발생")
    void joinGathering_모임없음_예외() {
        // given
        Long invalidGatheringId = 99999L;

        // when & then
        CustomException ex = assertThrows(CustomException.class, () ->
                participationService.join(invalidGatheringId, guestUser));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.GATHERING_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 참여한 유저가 다시 참여 시 ALREADY_JOINED 예외 발생")
    void joinGathering_중복참여_예외처리() {
        // given
        participationService.join(savedGathering.getId(), guestUser);

        // when & then
        CustomException ex = assertThrows(CustomException.class, () ->
                participationService.join(savedGathering.getId(), guestUser));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ALREADY_JOINED);
    }

    @Test
    @DisplayName("모임 참여 시 인원이 가득 찬 경우 CAPACITY_FULL 예외 발생")
    void joinGathering_정원초과_예외처리() {
        // given: 최대 정원 1명으로 설정된 모임 생성
        GatheringCreateRequestDto smallCapacityRequest = new GatheringCreateRequestDto(
                "정원초과 테스트 모임",
                "https://img.url",
                "스터디",
                1, // 정원 1명
                LocalDateTime.now().plusDays(1),
                "온라인",
                "https://open.kakao.com/test",
                "정원 초과 테스트용"
        );
        GatheringCreateResponseDto response = gatheringService.createGathering(smallCapacityRequest, hostUser);
        Long gatheringId = Long.parseLong(response.getId().replace("gath_", ""));
        Gathering smallGathering = gatheringRepository.findById(gatheringId).get();

        // 이미 호스트가 참여 중이므로 정원 꽉 참
        // when & then
        CustomException ex = assertThrows(CustomException.class, () ->
                participationService.join(gatheringId, guestUser));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CAPACITY_FULL);
    }

    @Test
    @DisplayName("모임 참여 취소 - 정상 취소")
    void cancelParticipation_정상취소() {
        // given: 게스트 유저가 모임에 참여함
        participationService.join(savedGathering.getId(), guestUser);

        // when: 게스트 유저가 참여 취소
        assertDoesNotThrow(() ->
                participationService.cancelParticipation(savedGathering.getId(), guestUser.getId()));

        // then: Participation 테이블에서 삭제되었는지 확인
        Optional<Participation> participationOpt =
                participationRepository.findByUserAndGathering(guestUser, savedGathering);
        assertThat(participationOpt).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 참여 취소 시 USER_NOT_FOUND 예외 발생")
    void cancelParticipation_존재하지않는유저_예외() {
        // given
        Long invalidUserId = 99999L;

        // when & then
        CustomException ex = assertThrows(CustomException.class, () ->
                participationService.cancelParticipation(savedGathering.getId(), invalidUserId));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}