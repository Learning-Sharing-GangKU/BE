package com.gangku.be.service.gathering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.gangku.be.constant.gathering.GatheringStatus;
import com.gangku.be.constant.participation.ParticipationRole;
import com.gangku.be.constant.participation.ParticipationStatus;
import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import com.gangku.be.dto.gathering.response.GatheringDetailResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.GatheringService;
import com.gangku.be.util.object.FileUrlResolver;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class GetGatheringDetailUnitTest {

    @Mock private GatheringRepository gatheringRepository;
    @Mock private ParticipationRepository participationRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileUrlResolver fileUrlResolver;

    @InjectMocks private GatheringService gatheringService;

    @Test
    @DisplayName("모임 상세 조회 (200 OK): 참여 테이블에 현재 유저와 모임 row가 존재하면 isJoined=true")
    void getGatheringDetail_isJoinedTrue() {
        // given
        Long gatheringId = 1L;
        Long userId = 10L;
        int page = 1;
        int size = 5;

        User host = User.builder().id(100L).nickname("호스트").build();
        User loginUser =
                User.builder()
                        .id(userId)
                        .nickname("참여유저")
                        .profileImageObjectKey("profiles/user10.png")
                        .build();

        Category category = Category.builder().id(1L).name("운동").build();

        Gathering gathering =
                Gathering.builder()
                        .id(gatheringId)
                        .host(host)
                        .category(category)
                        .title("주말 풋살")
                        .description("같이 운동해요")
                        .gatheringImageObjectKey("gatherings/g1.png")
                        .capacity(10)
                        .date(LocalDateTime.of(2026, 3, 25, 18, 0))
                        .location("서울")
                        .openChatUrl("https://open.kakao.com/test")
                        .status(GatheringStatus.RECRUITING)
                        .createdAt(LocalDateTime.of(2026, 3, 20, 12, 0))
                        .updatedAt(LocalDateTime.of(2026, 3, 21, 12, 0))
                        .build();

        Participation participation =
                Participation.builder()
                        .id(1L)
                        .user(loginUser)
                        .gathering(gathering)
                        .role(ParticipationRole.GUEST)
                        .status(ParticipationStatus.APPROVED)
                        .joinedAt(LocalDateTime.of(2026, 3, 21, 10, 0))
                        .build();

        Page<Participation> participationPage = new PageImpl<>(List.of(participation));

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(userRepository.findById(userId)).thenReturn(Optional.of(loginUser));
        when(participationRepository.existsByUserAndGathering(loginUser, gathering))
                .thenReturn(true);
        when(participationRepository.findByGatheringId(eq(gatheringId), any(Pageable.class)))
                .thenReturn(participationPage);
        when(fileUrlResolver.toPublicUrl("gatherings/g1.png"))
                .thenReturn("https://cdn.test/gatherings/g1.png");
        when(fileUrlResolver.toPublicUrl("profiles/user10.png"))
                .thenReturn("https://cdn.test/profiles/user10.png");

        // when
        GatheringDetailResponseDto response =
                gatheringService.getGatheringDetail(gatheringId, page, size, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isJoined()).isTrue();
        assertThat(response.getTitle()).isEqualTo("주말 풋살");
        assertThat(response.getCategory()).isEqualTo("운동");
        assertThat(response.getGatheringImageUrl()).isEqualTo("https://cdn.test/gatherings/g1.png");

        verify(gatheringRepository, times(1)).findById(gatheringId);
        verify(userRepository, times(1)).findById(userId);
        verify(participationRepository, times(1)).existsByUserAndGathering(loginUser, gathering);
        verify(participationRepository, times(1))
                .findByGatheringId(eq(gatheringId), any(Pageable.class));
        verify(fileUrlResolver, times(1)).toPublicUrl("gatherings/g1.png");
        verify(fileUrlResolver, atLeastOnce()).toPublicUrl("profiles/user10.png");
    }

    @Test
    @DisplayName("모임 상세 조회 (200 OK): 참여 테이블에 row가 없으면 isJoined=false")
    void getGatheringDetail_isJoinedFalse() {
        // given
        Long gatheringId = 1L;
        Long userId = 10L;
        int page = 1;
        int size = 5;

        User host = User.builder().id(100L).nickname("호스트").build();
        User loginUser = User.builder().id(userId).nickname("미참여유저").build();

        Category category = Category.builder().id(1L).name("스터디").build();

        Gathering gathering =
                Gathering.builder()
                        .id(gatheringId)
                        .host(host)
                        .category(category)
                        .title("백엔드 스터디")
                        .description("JPA 공부")
                        .capacity(8)
                        .date(LocalDateTime.of(2026, 3, 25, 19, 0))
                        .location("강남")
                        .status(GatheringStatus.RECRUITING)
                        .createdAt(LocalDateTime.of(2026, 3, 20, 12, 0))
                        .updatedAt(LocalDateTime.of(2026, 3, 21, 12, 0))
                        .build();

        Page<Participation> participationPage = new PageImpl<>(List.of());

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(userRepository.findById(userId)).thenReturn(Optional.of(loginUser));
        when(participationRepository.existsByUserAndGathering(loginUser, gathering))
                .thenReturn(false);
        when(participationRepository.findByGatheringId(eq(gatheringId), any(Pageable.class)))
                .thenReturn(participationPage);

        // when
        GatheringDetailResponseDto response =
                gatheringService.getGatheringDetail(gatheringId, page, size, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isJoined()).isFalse();
        assertThat(response.getTitle()).isEqualTo("백엔드 스터디");
        assertThat(response.getCategory()).isEqualTo("스터디");

        verify(gatheringRepository, times(1)).findById(gatheringId);
        verify(userRepository, times(1)).findById(userId);
        verify(participationRepository, times(1)).existsByUserAndGathering(loginUser, gathering);
        verify(participationRepository, times(1))
                .findByGatheringId(eq(gatheringId), any(Pageable.class));
        verify(fileUrlResolver, never()).toPublicUrl(anyString());
    }

    @Test
    @DisplayName("모임 상세 조회 (404 Not Found): 모임이 없으면 GATHERING_NOT_FOUND 예외")
    void getGatheringDetail_gatheringNotFound() {
        // given
        Long gatheringId = 999L;
        Long userId = 10L;
        int page = 1;
        int size = 5;

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                        () -> gatheringService.getGatheringDetail(gatheringId, page, size, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(GatheringErrorCode.GATHERING_NOT_FOUND);

        verify(gatheringRepository, times(1)).findById(gatheringId);
        verify(userRepository, never()).findById(anyLong());
        verify(participationRepository, never()).existsByUserAndGathering(any(), any());
        verify(participationRepository, never()).findByGatheringId(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("모임 상세 조회 (404 Not Found): 현재 로그인 유저가 없으면 USER_NOT_FOUND 예외")
    void getGatheringDetail_userNotFound() {
        // given
        Long gatheringId = 1L;
        Long userId = 999L;
        int page = 1;
        int size = 5;

        User host = User.builder().id(100L).nickname("호스트").build();
        Category category = Category.builder().id(1L).name("운동").build();

        Gathering gathering =
                Gathering.builder()
                        .id(gatheringId)
                        .host(host)
                        .category(category)
                        .title("러닝 모임")
                        .description("한강 러닝")
                        .build();

        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                        () -> gatheringService.getGatheringDetail(gatheringId, page, size, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        verify(gatheringRepository, times(1)).findById(gatheringId);
        verify(userRepository, times(1)).findById(userId);
        verify(participationRepository, never()).existsByUserAndGathering(any(), any());
        verify(participationRepository, never()).findByGatheringId(anyLong(), any(Pageable.class));
    }
}
