package com.gangku.be.service.gathering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import com.gangku.be.dto.gathering.request.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.response.GatheringResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.CategoryErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.service.command.GatheringCommandService;
import com.gangku.be.support.GatheringLookup;
import com.gangku.be.support.UserLookup;
import com.gangku.be.util.cache.HomeCache;
import com.gangku.be.util.object.FileUrlResolver;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
public class CreateGatheringCommandUnitTest {

    @Mock private GatheringRepository gatheringRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ParticipationRepository participationRepository;
    @Mock private UserLookup userLookup;
    @Mock private GatheringLookup gatheringLookup;
    @Mock private FileUrlResolver fileUrlResolver;
    @Mock private HomeCache homeCache;

    @InjectMocks private GatheringCommandService gatheringCommandService;

    @Test
    @DisplayName("모임 저장 성공: 유저/카테고리 유효하면 저장 성공")
    void saveGathering_success() throws Exception {
        // given
        Long hostId = 1L;
        GatheringCreateRequestDto requestDto =
                new GatheringCreateRequestDto(
                        "알고리즘 스터디",
                        "statics/image/prod/2025/11/efe6-a7d.jpg",
                        "study",
                        12,
                        LocalDateTime.of(2025, 10, 1, 10, 0),
                        "공학관 301",
                        "https://open.kakao.com/o/abcdef",
                        "기초부터 차근차근 알고리즘을 공부합니다.");

        User host = User.builder().id(hostId).participations(new ArrayList<>()).build();
        Category category = mock(Category.class);
        when(category.getName()).thenReturn("study");

        when(userLookup.findById(hostId)).thenReturn(host);
        when(categoryRepository.findByName("study")).thenReturn(Optional.of(category));
        when(gatheringRepository.save(any(Gathering.class)))
                .thenAnswer(
                        inv -> {
                            Gathering g = inv.getArgument(0);
                            Field idField = Gathering.class.getDeclaredField("id");
                            idField.setAccessible(true);
                            idField.set(g, 12345L);
                            return g;
                        });
        when(fileUrlResolver.toPublicUrl("statics/image/prod/2025/11/efe6-a7d.jpg"))
                .thenReturn("https://cdn.example.com/gatherings/2025/09/cover-uuid.jpg");

        // when
        GatheringResponseDto response = gatheringCommandService.saveGathering(requestDto, hostId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("gath_12345");
        assertThat(response.getTitle()).isEqualTo("알고리즘 스터디");
        assertThat(response.getGatheringImageUrl())
                .isEqualTo("https://cdn.example.com/gatherings/2025/09/cover-uuid.jpg");

        verify(userLookup, times(1)).findById(hostId);
        verify(categoryRepository, times(1)).findByName("study");
        verify(gatheringRepository, times(1)).save(any(Gathering.class));
        verify(participationRepository, times(1)).save(any(Participation.class));
        verify(fileUrlResolver, times(1)).toPublicUrl("statics/image/prod/2025/11/efe6-a7d.jpg");
        verify(homeCache, times(1)).invalidateHome();

        verifyNoMoreInteractions(
                userLookup,
                categoryRepository,
                gatheringRepository,
                participationRepository,
                fileUrlResolver,
                homeCache);
    }

    @Test
    @DisplayName("모임 저장 실패: 유저 없으면 USER_NOT_FOUND 예외")
    void saveGathering_userNotFound() {
        // given
        Long hostId = 1L;
        GatheringCreateRequestDto requestDto =
                new GatheringCreateRequestDto(
                        "알고리즘 스터디",
                        "statics/image/prod/2025/11/efe6-a7d.jpg",
                        "study",
                        12,
                        LocalDateTime.of(2025, 10, 1, 10, 0),
                        "공학관 301",
                        "https://open.kakao.com/o/abcdef",
                        "기초부터 차근차근 알고리즘을 공부합니다.");

        when(userLookup.findById(hostId))
                .thenThrow(new CustomException(UserErrorCode.USER_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> gatheringCommandService.saveGathering(requestDto, hostId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        verify(userLookup, times(1)).findById(hostId);
        verifyNoInteractions(categoryRepository, gatheringRepository, participationRepository);
        verifyNoMoreInteractions(userLookup);
    }

    @Test
    @DisplayName("모임 저장 실패: 카테고리 없으면 CATEGORY_NOT_FOUND 예외")
    void saveGathering_categoryNotFound() {
        // given
        Long hostId = 1L;
        GatheringCreateRequestDto requestDto =
                new GatheringCreateRequestDto(
                        "알고리즘 스터디",
                        "statics/image/prod/2025/11/efe6-a7d.jpg",
                        "study",
                        12,
                        LocalDateTime.of(2025, 10, 1, 10, 0),
                        "공학관 301",
                        "https://open.kakao.com/o/abcdef",
                        "기초부터 차근차근 알고리즘을 공부합니다.");

        User host = User.builder().id(hostId).participations(new ArrayList<>()).build();

        when(userLookup.findById(hostId)).thenReturn(host);
        when(categoryRepository.findByName("study")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> gatheringCommandService.saveGathering(requestDto, hostId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(CategoryErrorCode.CATEGORY_NOT_FOUND);

        verify(userLookup, times(1)).findById(hostId);
        verify(categoryRepository, times(1)).findByName("study");
        verifyNoInteractions(gatheringRepository, participationRepository);
        verifyNoMoreInteractions(userLookup, categoryRepository);
    }
}
