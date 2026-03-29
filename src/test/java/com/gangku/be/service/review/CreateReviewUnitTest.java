package com.gangku.be.service.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Review;
import com.gangku.be.domain.User;
import com.gangku.be.dto.ai.request.TextFilterRequestDto;
import com.gangku.be.dto.ai.response.TextFilterResponseDto;
import com.gangku.be.dto.review.ReviewCreateRequestDto;
import com.gangku.be.dto.review.ReviewCreateResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.exception.constant.ReviewErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.external.ai.AiApiClient;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.ReviewRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.ReviewService;
import com.gangku.be.util.ai.AiTextFilterMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class CreateReviewUnitTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private UserRepository userRepository;
    @Mock private GatheringRepository gatheringRepository;
    @Mock private ParticipationRepository participationRepository;
    @Mock private AiApiClient aiApiClient;
    @Mock private AiTextFilterMapper aiTextFilterMapper;

    @InjectMocks private ReviewService reviewService;

    @Test
    @DisplayName("리뷰 작성 (201 Created): 서로 다른 유저 + FINISHED 공통 모임 존재 시 리뷰 생성")
    void createReview_success() {
        // given
        Long reviewerId = 1L;
        Long revieweeId = 2L;
        Long gatheringId = 10L;

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(4, "좋았어요!");

        User reviewer = User.builder().id(reviewerId).build();
        User reviewee = User.builder().id(revieweeId).build();
        Gathering gathering = Gathering.builder().id(gatheringId).build();

        TextFilterRequestDto textFilterRequestDto = mock(TextFilterRequestDto.class);
        TextFilterResponseDto textFilterResponseDto = mock(TextFilterResponseDto.class);

        when(userRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(revieweeId)).thenReturn(Optional.of(reviewee));
        when(participationRepository.findFinishedCommonGatheringIds(reviewerId, revieweeId))
                .thenReturn(List.of(gatheringId));
        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(
                        inv -> {
                            Review r = inv.getArgument(0);
                            java.lang.reflect.Field f = Review.class.getDeclaredField("id");
                            f.setAccessible(true);
                            f.set(r, 1L);
                            return r;
                        });
        when(reviewRepository.existsByGatheringIdAndReviewerIdAndRevieweeId(
                        gatheringId, reviewerId, revieweeId))
                .thenReturn(false);
        when(aiTextFilterMapper.fromReviewCreate(requestDto)).thenReturn(textFilterRequestDto);
        when(aiApiClient.filterText(textFilterRequestDto)).thenReturn(textFilterResponseDto);
        when(textFilterResponseDto.isAllowed()).thenReturn(true);

        // when
        ReviewCreateResponseDto response =
                reviewService.createReview(reviewerId, revieweeId, requestDto);

        // then
        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository, times(1)).save(captor.capture());
        Review savedReview = captor.getValue();

        assertThat(savedReview.getReviewer()).isEqualTo(reviewer);
        assertThat(savedReview.getReviewee()).isEqualTo(reviewee);
        assertThat(savedReview.getGathering()).isEqualTo(gathering);
        assertThat(savedReview.getRating()).isEqualTo(4);
        assertThat(savedReview.getContent()).isEqualTo("좋았어요!");

        assertThat(response).isNotNull();

        verify(userRepository, times(1)).findById(reviewerId);
        verify(userRepository, times(1)).findById(revieweeId);
        verify(participationRepository, times(1))
                .findFinishedCommonGatheringIds(reviewerId, revieweeId);
        verify(gatheringRepository, times(1)).findById(gatheringId);
        verify(aiTextFilterMapper, times(1)).fromReviewCreate(requestDto);
        verify(aiApiClient, times(1)).filterText(textFilterRequestDto);

        verifyNoMoreInteractions(
                userRepository,
                participationRepository,
                gatheringRepository,
                reviewRepository,
                aiTextFilterMapper,
                aiApiClient);
    }

    @Test
    @DisplayName("리뷰 작성 (400 Bad Request): reviewerId와 revieweeId가 같으면 INVALID_REVIEW_TARGET 예외")
    void createReview_invalidTarget_sameUser() {
        // given
        Long reviewerId = 1L;
        Long revieweeId = 1L;

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(4, "좋았어요!");

        // when
        assertThatThrownBy(() -> reviewService.createReview(reviewerId, revieweeId, requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ReviewErrorCode.INVALID_REVIEW_TARGET);

        // then
        verifyNoInteractions(
                userRepository, participationRepository, gatheringRepository, reviewRepository);
    }

    @Test
    @DisplayName("리뷰 작성 (400 Bad Request): 리뷰 내용에 금칙어가 있으면 INVALID_REVIEW_CONTENT 예외")
    void createReview_invalidContent() {
        // given
        Long reviewerId = 1L;
        Long revieweeId = 2L;
        Long gatheringId = 10L;

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(4, "금칙어 포함 리뷰");

        User reviewer = User.builder().id(reviewerId).build();
        User reviewee = User.builder().id(revieweeId).build();
        Gathering gathering = Gathering.builder().id(gatheringId).build();

        TextFilterRequestDto textFilterRequestDto = mock(TextFilterRequestDto.class);
        TextFilterResponseDto textFilterResponseDto = mock(TextFilterResponseDto.class);

        when(userRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(revieweeId)).thenReturn(Optional.of(reviewee));
        when(participationRepository.findFinishedCommonGatheringIds(reviewerId, revieweeId))
                .thenReturn(List.of(gatheringId));
        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(reviewRepository.existsByGatheringIdAndReviewerIdAndRevieweeId(
                        gatheringId, reviewerId, revieweeId))
                .thenReturn(false);

        when(aiTextFilterMapper.fromReviewCreate(requestDto)).thenReturn(textFilterRequestDto);
        when(aiApiClient.filterText(textFilterRequestDto)).thenReturn(textFilterResponseDto);
        when(textFilterResponseDto.isAllowed()).thenReturn(false);

        // when
        assertThatThrownBy(() -> reviewService.createReview(reviewerId, revieweeId, requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ReviewErrorCode.INVALID_REVIEW_COMMENT);

        // then
        verify(userRepository, times(1)).findById(reviewerId);
        verify(userRepository, times(1)).findById(revieweeId);
        verify(participationRepository, times(1))
                .findFinishedCommonGatheringIds(reviewerId, revieweeId);
        verify(gatheringRepository, times(1)).findById(gatheringId);
        verify(reviewRepository, times(1))
                .existsByGatheringIdAndReviewerIdAndRevieweeId(gatheringId, reviewerId, revieweeId);
        verify(aiTextFilterMapper, times(1)).fromReviewCreate(requestDto);
        verify(aiApiClient, times(1)).filterText(textFilterRequestDto);

        verify(reviewRepository, never()).save(any());

        verifyNoMoreInteractions(
                userRepository,
                participationRepository,
                gatheringRepository,
                reviewRepository,
                aiTextFilterMapper,
                aiApiClient);
    }

    @Test
    @DisplayName("리뷰 작성 (404 Not Found): reviewer 유저가 없으면 USER_NOT_FOUND 예외")
    void createReview_reviewerNotFound() {
        // given
        Long reviewerId = 1L;
        Long revieweeId = 2L;

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(4, "좋았어요!");

        when(userRepository.findById(reviewerId)).thenReturn(Optional.empty());

        // when
        assertThatThrownBy(() -> reviewService.createReview(reviewerId, revieweeId, requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        // then
        verify(userRepository, times(1)).findById(reviewerId);
        verify(userRepository, never()).findById(revieweeId);

        verifyNoInteractions(participationRepository, gatheringRepository, reviewRepository);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("리뷰 작성 (404 Not Found): reviewee 유저가 없으면 USER_NOT_FOUND 예외")
    void createReview_revieweeNotFound() {
        // given
        Long reviewerId = 1L;
        Long revieweeId = 2L;

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(4, "좋았어요!");

        User reviewer = User.builder().id(reviewerId).build();

        when(userRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(revieweeId)).thenReturn(Optional.empty());

        // when
        assertThatThrownBy(() -> reviewService.createReview(reviewerId, revieweeId, requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        // then
        verify(userRepository, times(1)).findById(reviewerId);
        verify(userRepository, times(1)).findById(revieweeId);

        verifyNoInteractions(participationRepository, gatheringRepository, reviewRepository);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("리뷰 작성 (403 Forbidden): FINISHED 공통 모임이 없으면 NO_PERMISSION_TO_WRITE_REVIEW 예외")
    void createReview_noCommonFinishedGathering() {
        // given
        Long reviewerId = 1L;
        Long revieweeId = 2L;

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(4, "좋았어요!");

        User reviewer = User.builder().id(reviewerId).build();
        User reviewee = User.builder().id(revieweeId).build();

        when(userRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(revieweeId)).thenReturn(Optional.of(reviewee));
        when(participationRepository.findFinishedCommonGatheringIds(reviewerId, revieweeId))
                .thenReturn(List.of());

        // when
        assertThatThrownBy(() -> reviewService.createReview(reviewerId, revieweeId, requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ReviewErrorCode.NO_PERMISSION_TO_WRITE_REVIEW);

        // then
        verify(userRepository, times(1)).findById(reviewerId);
        verify(userRepository, times(1)).findById(revieweeId);
        verify(participationRepository, times(1))
                .findFinishedCommonGatheringIds(reviewerId, revieweeId);

        verifyNoInteractions(gatheringRepository, reviewRepository);
        verifyNoMoreInteractions(userRepository, participationRepository);
    }

    @Test
    @DisplayName("리뷰 작성 (404 Not Found): 공통 모임 id는 찾았지만 Gathering이 없으면 GATHERING_NOT_FOUND 예외")
    void createReview_gatheringNotFound() {
        // given
        Long reviewerId = 1L;
        Long revieweeId = 2L;
        Long gatheringId = 10L;

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(4, "좋았어요!");

        User reviewer = User.builder().id(reviewerId).build();
        User reviewee = User.builder().id(revieweeId).build();

        when(userRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(revieweeId)).thenReturn(Optional.of(reviewee));
        when(participationRepository.findFinishedCommonGatheringIds(reviewerId, revieweeId))
                .thenReturn(List.of(gatheringId));
        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.empty());

        // when
        assertThatThrownBy(() -> reviewService.createReview(reviewerId, revieweeId, requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(GatheringErrorCode.GATHERING_NOT_FOUND);

        // then
        verify(userRepository, times(1)).findById(reviewerId);
        verify(userRepository, times(1)).findById(revieweeId);
        verify(participationRepository, times(1))
                .findFinishedCommonGatheringIds(reviewerId, revieweeId);
        verify(gatheringRepository, times(1)).findById(gatheringId);

        verifyNoInteractions(reviewRepository);
        verifyNoMoreInteractions(userRepository, participationRepository, gatheringRepository);
    }

    @Test
    @DisplayName("리뷰 작성 (409 Conflict): 이미 같은 모임에 같은 대상 리뷰가 있으면 REVIEW_ALREADY_EXISTS 예외")
    void createReview_reviewAlreadyExists() {
        // given
        Long reviewerId = 1L;
        Long revieweeId = 2L;
        Long gatheringId = 10L;

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(4, "중복 리뷰 테스트");

        User reviewer = User.builder().id(reviewerId).build();
        User reviewee = User.builder().id(revieweeId).build();
        Gathering gathering = Gathering.builder().id(gatheringId).build();

        when(userRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(revieweeId)).thenReturn(Optional.of(reviewee));
        when(participationRepository.findFinishedCommonGatheringIds(reviewerId, revieweeId))
                .thenReturn(List.of(gatheringId));
        when(gatheringRepository.findById(gatheringId)).thenReturn(Optional.of(gathering));
        when(reviewRepository.existsByGatheringIdAndReviewerIdAndRevieweeId(
                        gatheringId, reviewerId, revieweeId))
                .thenReturn(true);

        // when
        assertThatThrownBy(() -> reviewService.createReview(reviewerId, revieweeId, requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ReviewErrorCode.REVIEW_ALREADY_EXISTS);

        // then
        verify(userRepository, times(1)).findById(reviewerId);
        verify(userRepository, times(1)).findById(revieweeId);
        verify(participationRepository, times(1))
                .findFinishedCommonGatheringIds(reviewerId, revieweeId);
        verify(gatheringRepository, times(1)).findById(gatheringId);

        verify(reviewRepository, times(1))
                .existsByGatheringIdAndReviewerIdAndRevieweeId(gatheringId, reviewerId, revieweeId);

        verify(reviewRepository, never()).save(any());

        verifyNoMoreInteractions(
                userRepository, participationRepository, gatheringRepository, reviewRepository);
    }
}
