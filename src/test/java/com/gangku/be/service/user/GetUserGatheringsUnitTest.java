package com.gangku.be.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.User;
import com.gangku.be.dto.gathering.response.GatheringListResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.CommonErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.GatheringService;
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
public class GetUserGatheringsUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private GatheringRepository gatheringRepository;
    @Mock private ParticipationRepository participationRepository;

    @InjectMocks private GatheringService gatheringService;

    @Test
    @DisplayName("유저가 만든 모임 조회 (200 OK): role=host이면 내가 만든 모임 목록 조회 성공")
    void getUserGatherings_host_success() {
        // given
        Long userId = 1L;
        String role = "host";
        int page = 1;
        int size = 10;

        User user =
                User.builder()
                        .id(userId)
                        .email("host@test.com")
                        .nickname("호스트")
                        .password("encoded")
                        .build();

        Category studyCategory = mock(Category.class);
        Category sportsCategory = mock(Category.class);

        when(studyCategory.getName()).thenReturn("STUDY");
        when(sportsCategory.getName()).thenReturn("SPORTS");

        Gathering gathering1 =
                Gathering.builder()
                        .id(101L)
                        .title("알고리즘 스터디")
                        .host(user)
                        .category(studyCategory)
                        .capacity(12)
                        .build();

        Gathering gathering2 =
                Gathering.builder()
                        .id(102L)
                        .title("주말 풋살")
                        .host(user)
                        .category(sportsCategory)
                        .capacity(10)
                        .build();
        Page<Gathering> gatheringPage = new PageImpl<>(List.of(gathering1, gathering2));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(gatheringRepository.findByHostId(eq(user), any(Pageable.class)))
                .thenReturn(gatheringPage);

        // when
        GatheringListResponseDto response =
                gatheringService.getUserGatherings(userId, role, page, size);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getMeta()).isNotNull();
        assertThat(response.getMeta().page()).isEqualTo(1);
        assertThat(response.getMeta().size()).isEqualTo(2);
        assertThat(response.getMeta().sortedBy()).isEqualTo("createdAt,desc");

        verify(userRepository, times(1)).findById(userId);
        verify(gatheringRepository, times(1)).findByHostId(eq(user), any(Pageable.class));
        verifyNoInteractions(participationRepository);
        verifyNoMoreInteractions(userRepository, gatheringRepository);
    }

    @Test
    @DisplayName("유저가 참가한 모임 조회 (200 OK): role=guest이면 내가 참가한 모임 목록 조회 성공")
    void getUserGatherings_guest_success() {
        // given
        Long userId = 1L;
        String role = "guest";
        int page = 1;
        int size = 10;

        User user =
                User.builder()
                        .id(userId)
                        .email("guest@test.com")
                        .nickname("게스트")
                        .password("encoded")
                        .build();

        Category studyCategory = mock(Category.class);
        Category sportsCategory = mock(Category.class);

        when(studyCategory.getName()).thenReturn("STUDY");
        when(sportsCategory.getName()).thenReturn("SPORTS");

        Gathering gathering1 =
                Gathering.builder()
                        .id(101L)
                        .title("알고리즘 스터디")
                        .host(user)
                        .category(studyCategory)
                        .capacity(12)
                        .build();

        Gathering gathering2 =
                Gathering.builder()
                        .id(102L)
                        .title("주말 풋살")
                        .host(user)
                        .category(sportsCategory)
                        .capacity(10)
                        .build();

        Page<Gathering> gatheringPage = new PageImpl<>(List.of(gathering1, gathering2));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(participationRepository.findJoinedGatheringsByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(gatheringPage);

        // when
        GatheringListResponseDto response =
                gatheringService.getUserGatherings(userId, role, page, size);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getMeta()).isNotNull();
        assertThat(response.getMeta().page()).isEqualTo(1);
        assertThat(response.getMeta().size()).isEqualTo(2);
        assertThat(response.getMeta().sortedBy()).isEqualTo("joinedAt,desc");

        verify(userRepository, times(1)).findById(userId);
        verify(participationRepository, times(1))
                .findJoinedGatheringsByUserId(eq(userId), any(Pageable.class));
        verifyNoInteractions(gatheringRepository);
        verifyNoMoreInteractions(userRepository, participationRepository);
    }

    @Test
    @DisplayName("유저가 만든/참가한 모임 조회 (404 Not Found): 대상 유저가 없으면 USER_NOT_FOUND 예외")
    void getUserGatherings_userNotFound() {
        // given
        Long userId = 999L;
        String role = "host";
        int page = 1;
        int size = 10;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when
        assertThatThrownBy(() -> gatheringService.getUserGatherings(userId, role, page, size))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        // then
        verify(userRepository, times(1)).findById(userId);
        verifyNoInteractions(gatheringRepository, participationRepository);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("유저가 만든/참가한 모임 조회 (400 Bad Request): role 값이 잘못되면 INVALID_REQUEST_PARAMETER 예외")
    void getUserGatherings_invalidRole() {
        // given
        Long userId = 1L;
        String role = "admin";
        int page = 1;
        int size = 10;

        User user =
                User.builder()
                        .id(userId)
                        .email("user@test.com")
                        .nickname("유저")
                        .password("encoded")
                        .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        assertThatThrownBy(() -> gatheringService.getUserGatherings(userId, role, page, size))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.INVALID_REQUEST_PARAMETER);

        // then
        verify(userRepository, times(1)).findById(userId);
        verifyNoInteractions(gatheringRepository, participationRepository);
        verifyNoMoreInteractions(userRepository);
    }
}
