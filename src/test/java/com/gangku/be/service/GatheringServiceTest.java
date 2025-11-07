package com.gangku.be.service;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import com.gangku.be.dto.gathering.request.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.request.GatheringUpdateRequestDto;
import com.gangku.be.dto.gathering.response.*;
import com.gangku.be.exception.CustomException;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class GatheringServiceTest {

    @Autowired
    private ParticipationService participationService;

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
                LocalDateTime.of(2026, 11, 13, 15, 0),
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
    @DisplayName("참여자 리스트 조회 - 페이지네이션 및 정렬 정상 동작")
    void getParticipants_success() {
        // given
        Category study = categoryRepository.findByName("스터디").orElseThrow();

        // 1️⃣ 모임 생성
        Gathering gathering = gatheringRepository.save(Gathering.builder()
                .title("스터디 모임")
                .category(study)
                .host(mockHost)
                .capacity(10)
                .participantCount(0)
                .date(LocalDateTime.now().plusDays(5))
                .location("건대입구 카페")
                .openChatUrl("https://open.kakao.com/o/study")
                .description("스터디 함께 해요")
                .build());

        // 2️⃣ 테스트용 참여자 5명 추가
        for (int i = 1; i <= 5; i++) {
            User user = userRepository.save(User.builder()
                    .email("user" + i + "@example.com")
                    .password("pw" + i)
                    .nickname("참여자" + i)
                    .photoUrl("https://cdn.example.com/profile" + i + ".jpg")
                    .build());

            Participation participation = Participation.builder()
                    .user(user)
                    .gathering(gathering)
                    .role(Participation.ParticipationRole.GUEST)
                    .status(Participation.Status.APPROVED)
                    .joinedAt(LocalDateTime.now().minusMinutes(i)) // i값 커질수록 늦게 참여
                    .build();
            participationRepository.save(participation);

            gathering.setParticipantCount(gathering.getParticipantCount() + 1);
        }
        gatheringRepository.save(gathering);

        // when
        var page1 = participationService.getParticipants(
                gathering.getId(),
                1, // page
                3, // size
                "joinedAt,asc"
        );

        // then
        assertThat(page1).isNotNull();
        assertThat(page1.getData()).hasSize(3); // 3명만 조회
        assertThat(page1.getMeta().getSize()).isEqualTo(3);
        assertThat(page1.getMeta().getPage()).isEqualTo(1);
        assertThat(page1.getMeta().getSortedBy()).isEqualTo("joinedAt,asc");
        assertThat(page1.getMeta().isHasNext()).isTrue(); // 남은 참여자 있음

        // 정렬 검증 (joinedAt 오름차순 → 먼저 참여한 사람이 먼저 나와야 함)
        LocalDateTime firstJoined = page1.getData().get(0).getJoinedAt();
        LocalDateTime secondJoined = page1.getData().get(1).getJoinedAt();
        assertThat(firstJoined).isBeforeOrEqualTo(secondJoined);
        // when - 두 번째 페이지 (page=2, size=3)
        var page2 = participationService.getParticipants(
                gathering.getId(),
                2,
                3,
                "joinedAt,asc"
        );
        // then - 2페이지 검증
        assertThat(page2).isNotNull();
        assertThat(page2.getData()).hasSize(2); // 남은 2명만 조회
        assertThat(page2.getMeta().getPage()).isEqualTo(2);
        assertThat(page2.getMeta().isHasPrev()).isTrue();
        assertThat(page2.getMeta().isHasNext()).isFalse();

        // 1페이지 마지막 참여자와 2페이지 첫 참여자 joinedAt 순서 검증
        LocalDateTime lastPage1 = page1.getData().get(2).getJoinedAt();
        LocalDateTime firstPage2 = page2.getData().get(0).getJoinedAt();
        assertThat(lastPage1).isBeforeOrEqualTo(firstPage2);
    }

    @Test
    @DisplayName("내가 만든 모임 리스트 조회 - role=host 성공")
    void getUserGatherings_host_success() {
        // given
        Category study = categoryRepository.findByName("스터디").orElseThrow();

        // 내가 만든 모임 2개 생성
        Gathering g1 = gatheringRepository.save(Gathering.builder()
                .title("알고리즘 스터디")
                .category(study)
                .host(mockHost)
                .capacity(10)
                .participantCount(5)
                .date(LocalDateTime.now().plusDays(3))
                .location("건대입구")
                .openChatUrl("https://open.kakao.com/o/study1")
                .description("기초 알고리즘")
                .imageUrl("https://cdn.example.com/101.jpg")
                .build());

        Gathering g2 = gatheringRepository.save(Gathering.builder()
                .title("자료구조 스터디")
                .category(study)
                .host(mockHost)
                .capacity(8)
                .participantCount(3)
                .date(LocalDateTime.now().plusDays(5))
                .location("강남")
                .openChatUrl("https://open.kakao.com/o/study2")
                .description("자료구조 스터디")
                .imageUrl("https://cdn.example.com/102.jpg")
                .build());

        // when
        var response = gatheringService.getUserGatherings(mockHost.getId(), "host", 10, null, "createdAt,desc");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getTitle()).isEqualTo("자료구조 스터디"); // 최신순 정렬 확인
        assertThat(response.getData().get(1).getTitle()).isEqualTo("알고리즘 스터디");
        assertThat(response.getMeta().getSortedBy()).isEqualTo("createdAt,desc");
    }

    @Test
    @DisplayName("내가 참여한 모임 리스트 조회 - role=guest 성공")
    void getUserGatherings_guest_success() {
        // given
        Category music = categoryRepository.findByName("음악").orElseThrow();

        // 다른 유저(호스트)
        User otherHost = userRepository.save(User.builder()
                .email("other@example.com")
                .password("pw")
                .nickname("다른호스트")
                .photoUrl("https://cdn.example.com/other.jpg")
                .build());

        // 다른 유저가 만든 모임
        Gathering g1 = gatheringRepository.save(Gathering.builder()
                .title("음악 감상 모임")
                .category(music)
                .host(otherHost)
                .capacity(10)
                .participantCount(1)
                .date(LocalDateTime.now().plusDays(1))
                .location("홍대")
                .openChatUrl("https://open.kakao.com/o/music1")
                .description("음악 감상")
                .build());

        // mockHost가 guest로 참여
        participationRepository.save(Participation.builder()
                .user(mockHost)
                .gathering(g1)
                .status(Participation.Status.APPROVED)
                .role(Participation.ParticipationRole.GUEST)
                .joinedAt(LocalDateTime.now())
                .build());

        // when
        var response = gatheringService.getUserGatherings(mockHost.getId(), "guest", 10, null, "createdAt,desc");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getTitle()).isEqualTo("음악 감상 모임");
        assertThat(response.getData().get(0).getHostName()).isEqualTo("다른호스트");
    }

    @Test
    @DisplayName("내 모임 조회 실패 - 잘못된 role 파라미터 (INVALID_ROLE)")
    void getUserGatherings_INVALID_ROLE() {
        assertThrows(CustomException.class, () ->
                gatheringService.getUserGatherings(mockHost.getId(), "wrongRole", 10, null, "createdAt,desc"));
    }


    /**
     * 홈화면 조회 테스트
     * - 최신/인기 모임을 각 3개씩 반환하는지 검증
     * - recommended는 현재 미구현 상태로 제외
     */
    @Test
    @DisplayName("홈화면 조회 - 최신/인기 모임 3개씩 정상 반환")
    void getHomeGatherings_success() {
        // given
        Category study = categoryRepository.findByName("스터디").orElseThrow();

        // 🔹 인기순용 모임들
        for (int i = 1; i <= 5; i++) {
            gatheringRepository.save(Gathering.builder()
                    .title("인기모임" + i)
                    .category(study)
                    .host(mockHost)
                    .capacity(10)
                    .participantCount(10 - i)
                    .date(LocalDateTime.now().plusDays(i))
                    .location("건대" + i)
                    .openChatUrl("https://open.kakao.com/o/popular" + i)
                    .description("인기 테스트용 모임")
                    .build());
        }

        // 🔹 최신순용 모임들
        for (int i = 1; i <= 5; i++) {
            gatheringRepository.save(Gathering.builder()
                    .title("최신모임" + i)
                    .category(study)
                    .host(mockHost)
                    .capacity(10)
                    .participantCount(i)
                    .date(LocalDateTime.now().plusDays(i))
                    .location("건대" + i)
                    .openChatUrl("https://open.kakao.com/o/latest" + i)
                    .description("최신 테스트용 모임")
                    .build());
        }

        // when
        GatheringListResponseDto latestResponse = gatheringService.getGatheringList(null, "latest", 3);
        GatheringListResponseDto popularResponse = gatheringService.getGatheringList(null, "popular", 3);

        // then
        assertThat(latestResponse).isNotNull();
        assertThat(popularResponse).isNotNull();

        // 최신순: 최근에 만든 모임이 맨 위
        assertThat(latestResponse.getData().getFirst().getTitle()).startsWith("최신모임");
        assertThat(latestResponse.getMeta().getSortedBy()).isEqualTo("createdAt,desc");

        // 인기순: 참여자 수가 많은 순으로 정렬되어야 함
        assertThat(popularResponse.getData().getFirst().getTitle()).startsWith("인기모임");
        assertThat(popularResponse.getMeta().getSortedBy()).isEqualTo("popularScore,desc");
    }

    /**
     * ✅ 카테고리 페이지 조회 테스트
     * - 특정 카테고리만 필터링되고 정렬 조건이 잘 적용되는지 검증
     */
    @Test
    @DisplayName("모임 리스트 조회 - 최신순 정렬 성공")
    void getGatheringList_latest_success() {
        // given
        Category sports = categoryRepository.findByName("운동").orElseThrow();
        Category study = categoryRepository.findByName("스터디").orElseThrow();

        // 운동 카테고리 모임
        gatheringRepository.save(Gathering.builder()
                .title("헬스 모임")
                .category(sports)
                .host(mockHost)
                .capacity(10)
                .participantCount(3)
                .date(LocalDateTime.of(2025, 11, 25, 18, 0))
                .location("스포애니")
                .openChatUrl("https://open.kakao.com/o/gym1")
                .description("운동 좋아하는 사람들")
                .build());

        gatheringRepository.save(Gathering.builder()
                .title("러닝 클럽")
                .category(sports)
                .host(mockHost)
                .capacity(20)
                .participantCount(10)
                .date(LocalDateTime.of(2025, 11, 26, 18, 0))
                .location("뚝섬유원지")
                .openChatUrl("https://open.kakao.com/o/run")
                .description("주말 러닝 모임")
                .build());

        // 다른 카테고리 모임
        gatheringRepository.save(Gathering.builder()
                .title("스터디 모임")
                .category(study)
                .host(mockHost)
                .capacity(10)
                .participantCount(5)
                .date(LocalDateTime.of(2025, 11, 27, 18, 0))
                .location("건대")
                .openChatUrl("https://open.kakao.com/o/study123")
                .description("스터디 모임")
                .build());


        // when
        GatheringListResponseDto response = gatheringService.getGatheringList("운동", "latest", 3);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().getFirst().getCategory()).isEqualTo("운동");
        assertThat(response.getMeta().getSortedBy()).isEqualTo("createdAt,desc");
    }

    @Test
    @DisplayName("모임 리스트 조회 - 인기순 정렬 성공")
    void getGatheringList_popular_success() {

        // given
        Category sports = categoryRepository.findByName("운동").orElseThrow();
        Category study = categoryRepository.findByName("스터디").orElseThrow();

        // 운동 카테고리 모임
        gatheringRepository.save(Gathering.builder()
                .title("헬스 모임")
                .category(sports)
                .host(mockHost)
                .capacity(10)
                .participantCount(3)
                .date(LocalDateTime.of(2025, 11, 25, 18, 0))
                .location("스포애니")
                .openChatUrl("https://open.kakao.com/o/gym1")
                .description("운동 좋아하는 사람들")
                .build());

        gatheringRepository.save(Gathering.builder()
                .title("러닝 클럽")
                .category(sports)
                .host(mockHost)
                .capacity(20)
                .participantCount(10)
                .date(LocalDateTime.of(2025, 11, 26, 18, 0))
                .location("뚝섬유원지")
                .openChatUrl("https://open.kakao.com/o/run")
                .description("주말 러닝 모임")
                .build());

        // 다른 카테고리 모임
        gatheringRepository.save(Gathering.builder()
                .title("스터디 모임")
                .category(study)
                .host(mockHost)
                .capacity(10)
                .participantCount(5)
                .date(LocalDateTime.of(2025, 11, 27, 18, 0))
                .location("건대")
                .openChatUrl("https://open.kakao.com/o/study123")
                .description("스터디 모임")
                .build());


        // when
        GatheringListResponseDto response = gatheringService.getGatheringList("운동", "popular", 3);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().getFirst().getCategory()).isEqualTo("운동");
        assertThat(response.getMeta().getSortedBy()).isEqualTo("participantCount,desc");
    }

    @Test
    @DisplayName("모임 리스트 조회 실패 - 잘못된 size 파라미터 (400 Bad Request)")
    void getGatheringList_invalid_size() {
        // when & then
        assertThrows(CustomException.class, () ->
                gatheringService.getGatheringList(null, "latest", 0)
        );
    }

    @Test
    @DisplayName("모임 리스트 조회 실패 - 존재하지 않는 카테고리 (404 Not Found)")
    void getGatheringList_category_not_found() {
        // when & then
        assertThrows(CustomException.class, () ->
                gatheringService.getGatheringList("없는카테고리", "latest", 3)
        );
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
                LocalDateTime.of(2026, 11, 3, 15, 0),
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