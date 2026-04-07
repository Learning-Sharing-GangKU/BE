package com.gangku.be.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.gangku.be.domain.Review;
import com.gangku.be.domain.User;
import com.gangku.be.dto.review.ReviewListResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.CommonErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.model.review.ReviewCursor;
import com.gangku.be.model.review.ReviewCursorCodec;
import com.gangku.be.repository.ReviewRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.UserService;
import com.gangku.be.util.object.FileUrlResolver;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class GetUserReviewsUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private FileUrlResolver fileUrlResolver;

    @InjectMocks private UserService userService;

    @Test
    @DisplayName("리뷰 더보기 조회 (200 OK): 공개된 유저의 리뷰 첫 페이지 조회 성공")
    void getUserReviews_success_firstPage() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 2L;
        int size = 3;
        String cursor = null;
        String sort = "createdAt,desc";

        User targetUser =
                User.builder()
                        .id(targetUserId)
                        .email("target@test.com")
                        .password("encoded")
                        .nickname("대상유저")
                        .reviewPublic(true)
                        .preferredCategories(new ArrayList<>())
                        .build();

        User reviewer1 =
                User.builder()
                        .id(10L)
                        .email("reviewer1@test.com")
                        .password("encoded")
                        .nickname("민수")
                        .profileImageObjectKey("profiles/reviewer1.png")
                        .preferredCategories(new ArrayList<>())
                        .build();

        User reviewer2 =
                User.builder()
                        .id(11L)
                        .email("reviewer2@test.com")
                        .password("encoded")
                        .nickname("지은")
                        .profileImageObjectKey("profiles/reviewer2.png")
                        .preferredCategories(new ArrayList<>())
                        .build();

        User reviewer3 =
                User.builder()
                        .id(12L)
                        .email("reviewer3@test.com")
                        .password("encoded")
                        .nickname("수진")
                        .profileImageObjectKey("profiles/reviewer3.png")
                        .preferredCategories(new ArrayList<>())
                        .build();

        User reviewer4 =
                User.builder()
                        .id(13L)
                        .email("reviewer4@test.com")
                        .password("encoded")
                        .nickname("철수")
                        .profileImageObjectKey("profiles/reviewer4.png")
                        .preferredCategories(new ArrayList<>())
                        .build();

        Review review1 =
                Review.builder()
                        .id(7L)
                        .reviewer(reviewer1)
                        .reviewee(targetUser)
                        .rating(4)
                        .content("유익했어요!")
                        .createdAt(LocalDateTime.of(2025, 9, 19, 18, 30, 0))
                        .updatedAt(LocalDateTime.of(2025, 9, 19, 18, 30, 0))
                        .build();

        Review review2 =
                Review.builder()
                        .id(6L)
                        .reviewer(reviewer2)
                        .reviewee(targetUser)
                        .rating(5)
                        .content("최고였습니다")
                        .createdAt(LocalDateTime.of(2025, 9, 19, 17, 10, 0))
                        .updatedAt(LocalDateTime.of(2025, 9, 19, 17, 10, 0))
                        .build();

        Review review3 =
                Review.builder()
                        .id(5L)
                        .reviewer(reviewer3)
                        .reviewee(targetUser)
                        .rating(3)
                        .content("괜찮았어요")
                        .createdAt(LocalDateTime.of(2025, 9, 19, 16, 0, 0))
                        .updatedAt(LocalDateTime.of(2025, 9, 19, 16, 0, 0))
                        .build();

        Review review4 =
                Review.builder()
                        .id(4L)
                        .reviewer(reviewer4)
                        .reviewee(targetUser)
                        .rating(2)
                        .content("보통이었어요")
                        .createdAt(LocalDateTime.of(2025, 9, 19, 15, 0, 0))
                        .updatedAt(LocalDateTime.of(2025, 9, 19, 15, 0, 0))
                        .build();

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(reviewRepository.findFirstPageByRevieweeId(eq(targetUserId), any(Pageable.class)))
                .thenReturn(List.of(review1, review2, review3, review4));

        when(fileUrlResolver.toPublicUrl("profiles/reviewer1.png"))
                .thenReturn("https://cdn.example.com/profiles/reviewer1.png");
        when(fileUrlResolver.toPublicUrl("profiles/reviewer2.png"))
                .thenReturn("https://cdn.example.com/profiles/reviewer2.png");
        when(fileUrlResolver.toPublicUrl("profiles/reviewer3.png"))
                .thenReturn("https://cdn.example.com/profiles/reviewer3.png");

        // when
        ReviewListResponseDto response =
                userService.getUserReviews(targetUserId, currentUserId, size, cursor);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getData()).hasSize(3);

        assertThat(response.getData().get(0).id()).isEqualTo("rev_7");
        assertThat(response.getData().get(0).reviewerId()).isEqualTo("usr_10");
        assertThat(response.getData().get(0).reviewerNickname()).isEqualTo("민수");
        assertThat(response.getData().get(0).reviewerProfileImageUrl())
                .isEqualTo("https://cdn.example.com/profiles/reviewer1.png");

        assertThat(response.getMeta().size()).isEqualTo(3);
        assertThat(response.getMeta().sortedBy()).isEqualTo("createdAt,desc");
        assertThat(response.getMeta().hasNext()).isTrue();
        assertThat(response.getMeta().nextCursor()).isNotBlank();

        verify(userRepository, times(1)).findById(targetUserId);
        verify(reviewRepository, times(1))
                .findFirstPageByRevieweeId(eq(targetUserId), any(Pageable.class));
        verify(fileUrlResolver, times(1)).toPublicUrl("profiles/reviewer1.png");
        verify(fileUrlResolver, times(1)).toPublicUrl("profiles/reviewer2.png");
        verify(fileUrlResolver, times(1)).toPublicUrl("profiles/reviewer3.png");
        verifyNoMoreInteractions(userRepository, reviewRepository, fileUrlResolver);
    }

    @Test
    @DisplayName("리뷰 더보기 조회 (200 OK): cursor 기반 다음 페이지 조회 성공")
    void getUserReviews_success_nextPage() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 2L;
        int size = 2;
        String sort = "createdAt,desc";

        User targetUser =
                User.builder()
                        .id(targetUserId)
                        .email("target@test.com")
                        .password("encoded")
                        .nickname("대상유저")
                        .reviewPublic(true)
                        .preferredCategories(new ArrayList<>())
                        .build();

        User reviewer1 =
                User.builder()
                        .id(20L)
                        .email("reviewer1@test.com")
                        .password("encoded")
                        .nickname("민수")
                        .profileImageObjectKey("profiles/reviewer1.png")
                        .preferredCategories(new ArrayList<>())
                        .build();

        User reviewer2 =
                User.builder()
                        .id(21L)
                        .email("reviewer2@test.com")
                        .password("encoded")
                        .nickname("지은")
                        .profileImageObjectKey("profiles/reviewer2.png")
                        .preferredCategories(new ArrayList<>())
                        .build();

        User reviewer3 =
                User.builder()
                        .id(22L)
                        .email("reviewer3@test.com")
                        .password("encoded")
                        .nickname("수진")
                        .profileImageObjectKey("profiles/reviewer3.png")
                        .preferredCategories(new ArrayList<>())
                        .build();

        Review review1 =
                Review.builder()
                        .id(5L)
                        .reviewer(reviewer1)
                        .reviewee(targetUser)
                        .rating(5)
                        .content("좋았어요")
                        .createdAt(LocalDateTime.of(2025, 9, 19, 16, 0, 0))
                        .updatedAt(LocalDateTime.of(2025, 9, 19, 16, 0, 0))
                        .build();

        Review review2 =
                Review.builder()
                        .id(4L)
                        .reviewer(reviewer2)
                        .reviewee(targetUser)
                        .rating(4)
                        .content("괜찮았어요")
                        .createdAt(LocalDateTime.of(2025, 9, 19, 15, 0, 0))
                        .updatedAt(LocalDateTime.of(2025, 9, 19, 15, 0, 0))
                        .build();

        Review review3 =
                Review.builder()
                        .id(3L)
                        .reviewer(reviewer3)
                        .reviewee(targetUser)
                        .rating(3)
                        .content("보통이었어요")
                        .createdAt(LocalDateTime.of(2025, 9, 19, 14, 0, 0))
                        .updatedAt(LocalDateTime.of(2025, 9, 19, 14, 0, 0))
                        .build();

        String cursor =
                ReviewCursorCodec.encode(
                        new ReviewCursor(LocalDateTime.of(2025, 9, 19, 17, 10, 0), 6L));

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(reviewRepository.findNextPageByRevieweeIdAndCursorDesc(
                        eq(targetUserId),
                        eq(LocalDateTime.of(2025, 9, 19, 17, 10, 0)),
                        eq(6L),
                        any(Pageable.class)))
                .thenReturn(List.of(review1, review2, review3));

        when(fileUrlResolver.toPublicUrl("profiles/reviewer1.png"))
                .thenReturn("https://cdn.example.com/profiles/reviewer1.png");
        when(fileUrlResolver.toPublicUrl("profiles/reviewer2.png"))
                .thenReturn("https://cdn.example.com/profiles/reviewer2.png");

        // when
        ReviewListResponseDto response =
                userService.getUserReviews(targetUserId, currentUserId, size, cursor);

        // then
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).id()).isEqualTo("rev_5");
        assertThat(response.getData().get(1).id()).isEqualTo("rev_4");
        assertThat(response.getMeta().hasNext()).isTrue();
        assertThat(response.getMeta().nextCursor()).isNotBlank();

        verify(userRepository, times(1)).findById(targetUserId);
        verify(reviewRepository, times(1))
                .findNextPageByRevieweeIdAndCursorDesc(
                        eq(targetUserId),
                        eq(LocalDateTime.of(2025, 9, 19, 17, 10, 0)),
                        eq(6L),
                        any(Pageable.class));
        verify(fileUrlResolver, times(1)).toPublicUrl("profiles/reviewer1.png");
        verify(fileUrlResolver, times(1)).toPublicUrl("profiles/reviewer2.png");
        verifyNoMoreInteractions(userRepository, reviewRepository, fileUrlResolver);
    }

    @Test
    @DisplayName("리뷰 더보기 조회 (404 Not Found): 대상 유저가 없으면 USER_NOT_FOUND 예외")
    void getUserReviews_userNotFound() {
        // given
        Long targetUserId = 999L;
        Long currentUserId = 1L;
        int size = 3;
        String cursor = null;
        String sort = "createdAt,desc";

        when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

        // when
        assertThatThrownBy(
                        () -> userService.getUserReviews(targetUserId, currentUserId, size, cursor))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        // then
        verify(userRepository, times(1)).findById(targetUserId);
        verifyNoInteractions(reviewRepository, fileUrlResolver);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("리뷰 더보기 조회 (403 Forbidden): 비공개 리뷰를 타인이 조회하면 NO_PERMISSION_TO_VIEW_REVIEW 예외")
    void getUserReviews_noPermissionToViewReview() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 2L;
        int size = 3;
        String cursor = null;
        String sort = "createdAt,desc";

        User targetUser =
                User.builder()
                        .id(targetUserId)
                        .email("target@test.com")
                        .password("encoded")
                        .nickname("대상유저")
                        .reviewPublic(false)
                        .preferredCategories(new ArrayList<>())
                        .build();

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        // when
        assertThatThrownBy(
                        () -> userService.getUserReviews(targetUserId, currentUserId, size, cursor))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.NO_PERMISSION_TO_VIEW_REVIEW);

        // then
        verify(userRepository, times(1)).findById(targetUserId);
        verifyNoInteractions(reviewRepository, fileUrlResolver);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("리뷰 더보기 조회 (400 Bad Request): cursor 값이 잘못되면 INVALID_REQUEST_PARAMETER 예외")
    void getUserReviews_invalidCursor() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 1L;
        int size = 3;
        String cursor = "invalid-cursor";

        User targetUser =
                User.builder()
                        .id(targetUserId)
                        .email("target@test.com")
                        .password("encoded")
                        .nickname("대상유저")
                        .reviewPublic(true)
                        .preferredCategories(new ArrayList<>())
                        .build();

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        // when
        assertThatThrownBy(
                        () -> userService.getUserReviews(targetUserId, currentUserId, size, cursor))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.INVALID_REQUEST_PARAMETER);

        // then
        verify(userRepository, times(1)).findById(targetUserId);
        verifyNoInteractions(reviewRepository, fileUrlResolver);
        verifyNoMoreInteractions(userRepository);
    }
}
