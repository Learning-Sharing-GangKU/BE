package com.gangku.be.service;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.User;
import com.gangku.be.dto.gathering.request.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.request.GatheringUpdateRequestDto;
import com.gangku.be.dto.gathering.response.GatheringCreateResponseDto;
import com.gangku.be.dto.gathering.response.GatheringDetailResponseDto;
import com.gangku.be.dto.gathering.response.GatheringUpdateResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class GatheringServiceTest {

    @Autowired
    private GatheringService gatheringService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private GatheringRepository gatheringRepository;

    @Autowired
    private ParticipationRepository participationRepository;

    private User mockHost;

    @BeforeEach
    void setup() {
        // 🔸 mockHost: 실제 DB에 저장
        mockHost = userRepository.save(User.builder()
                .email("host@example.com")
                .password("encoded_pw")
                .nickname("테스트호스트")
                .photoUrl("https://cdn.example.com/default-profile.jpg")
                .build());

        List<String> categoryNames = List.of("스터디", "운동", "음악", "영화", "게임");

        // 🔸 카테고리도 DB에 저장
        for (String name : categoryNames) {
            categoryRepository.findByName(name)
                    .orElseGet(() -> categoryRepository.save(
                            Category.builder().name(name).build()
                    ));
        }
    }

    @Test
    @DisplayName("모임이 정상적으로 생성되고 호스트도 자동 참여자로 등록되어야 한다")
    void createGathering_정상생성() {
        // given
        GatheringCreateRequestDto request = new GatheringCreateRequestDto(
                "모임 제목입니다",
                "https://img.url",
                "스터디",
                10,
                LocalDateTime.of(2025, 11, 5, 15, 0),
                "건대입구역 근처 카페",
                "https://open.kakao.com/test-room",
                "이건 모임 설명입니다"
        );

        // when
        GatheringCreateResponseDto response = gatheringService.createGathering(request, mockHost);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("모임 제목입니다");
        assertThat(response.getCategory()).isEqualTo("스터디");
        assertThat(response.getHostId()).isEqualTo("usr_" + mockHost.getId());

        // 저장된 모임 확인
        Long gatheringId = Long.parseLong(response.getId().replace("gath_", ""));
        Gathering saved = gatheringRepository.findById(gatheringId).orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("모임 제목입니다");
        assertThat(saved.getHost().getId()).isEqualTo(mockHost.getId());

        // 호스트가 참여자로 등록되었는지 확인
        assertThat(participationRepository.existsByUserAndGathering(mockHost, saved)).isTrue();
    }

    @Test
    @DisplayName("모임 생성 실패 - 유효하지 않은 필드 값 (예: 제목 너무 짧음)")
    void createGathering_INVALID_FIELD_VALUE() {
        GatheringCreateRequestDto invalidRequest = new GatheringCreateRequestDto(
                "", // 제목 없음
                "invalid-url", // 잘못된 URL
                "허용되지않은카테고리", // 존재하지 않는 카테고리
                0, // 잘못된 인원
                LocalDateTime.of(2020, 1, 1, 0, 0), // 과거 날짜
                "", // 빈 위치
                "http://open.kakao.com", // https 아님
                "a".repeat(1001) // 너무 긴 설명
        );

        assertThrows(CustomException.class, () ->
                gatheringService.createGathering(invalidRequest, mockHost));
    }

    @Test
    @DisplayName("모임 상세조회 - 유효한 ID로 정상 조회된다")
    void getGatheringById_정상조회() {
        // given
        GatheringCreateRequestDto createRequest = new GatheringCreateRequestDto(
                "상세조회 테스트 모임",
                "https://cdn.example.com/detail.jpg",
                "스터디",
                10,
                LocalDateTime.of(2025, 11, 10, 15, 0),
                "서울시 광진구",
                "https://open.kakao.com/o/detailRoom",
                "상세조회용 모임 설명"
        );
        GatheringCreateResponseDto createResponse = gatheringService.createGathering(createRequest, mockHost);
        Long gatheringId = Long.parseLong(createResponse.getId().replace("gath_", ""));

        // when
        GatheringDetailResponseDto detailResponse = gatheringService.getGatheringById(gatheringId, mockHost.getId());

        // then
        assertThat(detailResponse).isNotNull();
        assertThat(detailResponse.getId()).isEqualTo("gath_" + gatheringId);
        assertThat(detailResponse.getTitle()).isEqualTo("상세조회 테스트 모임");
        assertThat(detailResponse.getCategory()).isEqualTo("스터디");
        assertThat(detailResponse.getHost().getNickname()).isEqualTo("테스트호스트");
        assertThat(detailResponse.getParticipantsPreview().getData()).isNotEmpty();
        assertThat(detailResponse.getParticipantsPreview().getMeta().getTotalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("모임 상세조회 실패 - 존재하지 않는 ID")
    void getGatheringById_GATHERING_NOT_FOUND() {
        assertThrows(CustomException.class, () ->
                gatheringService.getGatheringById(99999L, mockHost.getId()));
    }

    @Test
    @DisplayName("모임 상세조회 실패 - 잘못된 ID (0 이하)")
    void getGatheringById_INVALID_GATHERING_ID() {
        assertThrows(CustomException.class, () ->
                gatheringService.getGatheringById(0L, mockHost.getId()));
    }


    @Test
    @Transactional
    @DisplayName("모임 정보를 호스트가 정상적으로 수정할 수 있어야 한다")
    void updateGathering_정상수정() {
        //  모임 생성
        GatheringCreateRequestDto createRequest = new GatheringCreateRequestDto(
                "오리지널 제목",
                "https://cdn.example.com/original.jpg",
                "스터디",
                10,
                LocalDateTime.of(2025, 11, 3, 15, 0),
                "강의동 101호",
                "https://open.kakao.com/o/original",
                "오리지널 설명"
        );

        GatheringCreateResponseDto createResponse = gatheringService.createGathering(createRequest, mockHost);
        Long gatheringId = Long.parseLong(createResponse.getId().replace("gath_", ""));

        // 수정 요청
        GatheringUpdateRequestDto updateRequest = GatheringUpdateRequestDto.builder()
                .title("수정된 제목")
                .imageUrl("https://cdn.example.com/updated.jpg")
                .category("운동")
                .capacity(15)
                .date(LocalDateTime.of(2025, 12, 25, 18, 0))
                .location("운동장 앞")
                .openChatUrl("https://open.kakao.com/o/updated")
                .description("수정된 설명")
                .build();

        GatheringUpdateResponseDto updateResponse = gatheringService.updateGathering(gatheringId, mockHost.getId(), updateRequest);

        //  검증
        assertThat(updateResponse.getTitle()).isEqualTo("수정된 제목");
        assertThat(updateResponse.getImageUrl()).isEqualTo("https://cdn.example.com/updated.jpg");
        assertThat(updateResponse.getCategory()).isEqualTo("운동");
        assertThat(updateResponse.getCapacity()).isEqualTo(15);
        assertThat(updateResponse.getLocation()).isEqualTo("운동장 앞");
        assertThat(updateResponse.getOpenChatUrl()).isEqualTo("https://open.kakao.com/o/updated");
        assertThat(updateResponse.getDescription()).isEqualTo("수정된 설명");

        Gathering updated = gatheringRepository.findById(gatheringId).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("수정된 제목");
        assertThat(updated.getCategory().getName()).isEqualTo("운동");
    }


    @Test
    @DisplayName("모임 수정 실패 - 존재하지 않는 모임 ID")
    void updateGathering_GATHERING_NOT_FOUND() {
        GatheringUpdateRequestDto updateDto = GatheringUpdateRequestDto.builder()
                .title("업데이트 제목")
                .build();

        assertThrows(CustomException.class, () ->
                gatheringService.updateGathering(99999L, mockHost.getId(), updateDto));
    }

    @Test
    @DisplayName("모임 수정 실패 - 호스트가 아닌 사용자가 요청")
    void updateGathering_FORBIDDEN() {
        // 모임 생성
        GatheringCreateRequestDto request = new GatheringCreateRequestDto(
                "모임 제목",
                "https://img.url",
                "스터디",
                10,
                LocalDateTime.of(2025, 11, 10, 18, 0),
                "장소",
                "https://open.kakao.com/o/test",
                "설명"
        );
        GatheringCreateResponseDto response = gatheringService.createGathering(request, mockHost);
        Long gatheringId = Long.parseLong(response.getId().replace("gath_", ""));

        // 다른 사용자 생성
        User stranger = userRepository.save(User.builder()
                .email("stranger@example.com")
                .password("pw")
                .nickname("낯선이")
                .photoUrl("https://cdn.example.com/stranger.jpg")
                .build());

        GatheringUpdateRequestDto updateDto = GatheringUpdateRequestDto.builder()
                .title("낯선이의 수정")
                .build();

        assertThrows(CustomException.class, () ->
                gatheringService.updateGathering(gatheringId, stranger.getId(), updateDto));
    }

    @DisplayName("모임을 호스트가 삭제하면 정상적으로 삭제된다")
    @Test
    void deleteGathering_정상삭제() {
        // given
        GatheringCreateRequestDto request = new GatheringCreateRequestDto(
                "삭제할 모임",
                "https://image.url",
                "스터디",
                10,
                LocalDateTime.of(2025, 12, 1, 18, 0),
                "건대역 1번출구",
                "https://open.kakao.com/o/deleteRoom",
                "삭제 테스트용 모임"
        );
        GatheringCreateResponseDto response = gatheringService.createGathering(request, mockHost);
        Long gatheringId = Long.parseLong(response.getId().replace("gath_", ""));


        // when
        gatheringService.deleteGathering(gatheringId, mockHost.getId());

        // then
        assertThat(gatheringRepository.findById(gatheringId)).isEmpty();
    }


    @Test
    @DisplayName("모임 삭제 실패 - 존재하지 않는 모임 ID")
    void deleteGathering_GATHERING_NOT_FOUND() {
        assertThrows(CustomException.class, () ->
                gatheringService.deleteGathering(99999L, mockHost.getId()));
    }

    @Test
    @DisplayName("모임 삭제 실패 - 호스트가 아닌 사용자가 삭제 요청")
    void deleteGathering_FORBIDDEN() {
        GatheringCreateRequestDto request = new GatheringCreateRequestDto(
                "삭제용 모임",
                "https://image.url",
                "스터디",
                10,
                LocalDateTime.of(2025, 12, 1, 18, 0),
                "건대역",
                "https://open.kakao.com/o/delete",
                "삭제 테스트"
        );
        GatheringCreateResponseDto response = gatheringService.createGathering(request, mockHost);
        Long gatheringId = Long.parseLong(response.getId().replace("gath_", ""));

        User stranger = userRepository.save(User.builder()
                .email("stranger2@example.com")
                .password("pw")
                .nickname("낯선2")
                .photoUrl("https://cdn.example.com/stranger2.jpg")
                .build());

        assertThrows(CustomException.class, () ->
                gatheringService.deleteGathering(gatheringId, stranger.getId()));
    }
}