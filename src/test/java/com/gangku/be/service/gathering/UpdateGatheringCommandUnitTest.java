package com.gangku.be.service.gathering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.gangku.be.constant.gathering.GatheringStatus;
import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.User;
import com.gangku.be.dto.gathering.request.GatheringUpdateRequestDto;
import com.gangku.be.dto.gathering.response.GatheringResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.CategoryErrorCode;
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.service.command.GatheringCommandService;
import com.gangku.be.support.GatheringLookup;
import com.gangku.be.support.UserLookup;
import com.gangku.be.util.cache.HomeCache;
import com.gangku.be.util.object.FileUrlResolver;
import java.time.LocalDateTime;
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
public class UpdateGatheringCommandUnitTest {

    @Mock private GatheringRepository gatheringRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ParticipationRepository participationRepository;
    @Mock private UserLookup userLookup;
    @Mock private GatheringLookup gatheringLookup;
    @Mock private FileUrlResolver fileUrlResolver;
    @Mock private HomeCache homeCache;

    @InjectMocks private GatheringCommandService gatheringCommandService;

    @Test
    @DisplayName("모임 수정 성공: 호스트가 수정 요청하면 모임 수정 성공")
    void updateGathering_success() {
        // given
        Long gatheringId = 1L;
        Long userId = 10L;

        User host = User.builder().id(userId).build();
        Category oldCategory = mock(Category.class);
        Category newCategory = mock(Category.class);
        when(newCategory.getName()).thenReturn("study");

        Gathering gathering =
                Gathering.builder()
                        .id(gatheringId)
                        .host(host)
                        .category(oldCategory)
                        .title("기존 제목")
                        .gatheringImageObjectKey("statics/image/prod/2025/11/old.jpg")
                        .capacity(10)
                        .date(LocalDateTime.of(2025, 10, 1, 10, 0))
                        .location("공학관 301")
                        .openChatUrl("https://open.kakao.com/o/old")
                        .description("기존 설명")
                        .status(GatheringStatus.RECRUITING)
                        .build();

        GatheringUpdateRequestDto requestDto =
                new GatheringUpdateRequestDto(
                        "제목 수정",
                        "statics/image/prod/2025/11/new.jpg",
                        "study",
                        15,
                        LocalDateTime.of(2025, 10, 5, 10, 0),
                        "공학관 302",
                        "https://open.kakao.com/o/xyz987",
                        "설명 업데이트");

        when(gatheringLookup.findById(gatheringId)).thenReturn(gathering);
        when(categoryRepository.findByName("study")).thenReturn(Optional.of(newCategory));
        when(gatheringRepository.save(gathering)).thenReturn(gathering);
        when(fileUrlResolver.toPublicUrl("statics/image/prod/2025/11/new.jpg"))
                .thenReturn("https://cdn.example.com/gatherings/2025/09/new-cover.jpg");

        // when
        GatheringResponseDto response =
                gatheringCommandService.updateGathering(gatheringId, userId, requestDto);

        // then
        assertThat(gathering.getTitle()).isEqualTo("제목 수정");
        assertThat(gathering.getGatheringImageObjectKey())
                .isEqualTo("statics/image/prod/2025/11/new.jpg");
        assertThat(gathering.getCategory()).isEqualTo(newCategory);
        assertThat(gathering.getCapacity()).isEqualTo(15);
        assertThat(gathering.getDate()).isEqualTo(LocalDateTime.of(2025, 10, 5, 10, 0));
        assertThat(gathering.getLocation()).isEqualTo("공학관 302");
        assertThat(gathering.getOpenChatUrl()).isEqualTo("https://open.kakao.com/o/xyz987");
        assertThat(gathering.getDescription()).isEqualTo("설명 업데이트");

        assertThat(response.getId()).isEqualTo("gath_1");
        assertThat(response.getTitle()).isEqualTo("제목 수정");

        verify(gatheringLookup, times(1)).findById(gatheringId);
        verify(categoryRepository, times(1)).findByName("study");
        verify(gatheringRepository, times(1)).save(gathering);
        verify(fileUrlResolver, times(1)).toPublicUrl("statics/image/prod/2025/11/new.jpg");
        verify(homeCache, times(1)).invalidateHome();

        verifyNoMoreInteractions(
                gatheringRepository, categoryRepository, fileUrlResolver, homeCache);
        verifyNoInteractions(userLookup, participationRepository);
    }

    @Test
    @DisplayName("모임 수정 실패: 모임 없으면 GATHERING_NOT_FOUND 예외")
    void updateGathering_notFound() {
        // given
        Long gatheringId = 1L;
        Long userId = 10L;

        GatheringUpdateRequestDto requestDto =
                new GatheringUpdateRequestDto(
                        "제목 수정",
                        "statics/image/prod/2025/11/new.jpg",
                        "study",
                        15,
                        LocalDateTime.of(2025, 10, 5, 10, 0),
                        "공학관 302",
                        "https://open.kakao.com/o/xyz987",
                        "설명 업데이트");

        when(gatheringLookup.findById(gatheringId))
                .thenThrow(new CustomException(GatheringErrorCode.GATHERING_NOT_FOUND));

        // when & then
        assertThatThrownBy(
                        () ->
                                gatheringCommandService.updateGathering(
                                        gatheringId, userId, requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(GatheringErrorCode.GATHERING_NOT_FOUND);

        verify(gatheringLookup, times(1)).findById(gatheringId);
        verifyNoInteractions(categoryRepository, userLookup, participationRepository);
        verifyNoMoreInteractions(gatheringLookup);
    }

    @Test
    @DisplayName("모임 수정 실패: 호스트 아니면 NO_PERMISSION_TO_MANIPULATE_GATHERING 예외")
    void updateGathering_noPermission() {
        // given
        Long gatheringId = 1L;
        Long userId = 10L;
        Long hostId = 20L;

        User host = User.builder().id(hostId).build();
        Gathering gathering = Gathering.builder().id(gatheringId).host(host).build();

        GatheringUpdateRequestDto requestDto =
                new GatheringUpdateRequestDto(
                        "제목 수정",
                        "statics/image/prod/2025/11/new.jpg",
                        "study",
                        15,
                        LocalDateTime.of(2025, 10, 5, 10, 0),
                        "공학관 302",
                        "https://open.kakao.com/o/xyz987",
                        "설명 업데이트");

        when(gatheringLookup.findById(gatheringId)).thenReturn(gathering);

        // when & then
        assertThatThrownBy(
                        () ->
                                gatheringCommandService.updateGathering(
                                        gatheringId, userId, requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(GatheringErrorCode.NO_PERMISSION_TO_MANIPULATE_GATHERING);

        verify(gatheringLookup, times(1)).findById(gatheringId);
        verifyNoInteractions(categoryRepository, userLookup, participationRepository);
        verifyNoMoreInteractions(gatheringLookup);
    }

    @Test
    @DisplayName("모임 수정 실패: 카테고리 없으면 CATEGORY_NOT_FOUND 예외")
    void updateGathering_categoryNotFound() {
        // given
        Long gatheringId = 1L;
        Long userId = 10L;

        User host = User.builder().id(userId).build();
        Category oldCategory = mock(Category.class);

        Gathering gathering =
                Gathering.builder()
                        .id(gatheringId)
                        .host(host)
                        .category(oldCategory)
                        .title("기존 제목")
                        .description("기존 설명")
                        .build();

        GatheringUpdateRequestDto requestDto =
                new GatheringUpdateRequestDto(
                        "제목 수정", null, "study", null, null, null, null, "설명 수정");

        when(gatheringLookup.findById(gatheringId)).thenReturn(gathering);
        when(categoryRepository.findByName("study")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                        () ->
                                gatheringCommandService.updateGathering(
                                        gatheringId, userId, requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(CategoryErrorCode.CATEGORY_NOT_FOUND);

        verify(gatheringLookup, times(1)).findById(gatheringId);
        verify(categoryRepository, times(1)).findByName("study");
        verify(gatheringRepository, never()).save(any());
        verifyNoMoreInteractions(gatheringRepository, categoryRepository);
        verifyNoInteractions(userLookup, participationRepository);
    }
}
