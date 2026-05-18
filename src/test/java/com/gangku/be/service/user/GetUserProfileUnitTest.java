package com.gangku.be.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Review;
import com.gangku.be.domain.User;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.UserErrorCode;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class GetUserProfileUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private FileUrlResolver fileUrlResolver;

    @InjectMocks private UserService userService;

    // ──────────────────────────────────────────────
    // 공통 픽스처 헬퍼
    // ──────────────────────────────────────────────

    private User buildTargetUser(Long id, boolean reviewPublic) {
        return User.builder()
                .id(id)
                .nickname("진지충")
                .age(23)
                .gender("MALE")
                .enrollNumber(22)
                .profileImageObjectKey("profiles/user1.png")
                .reviewPublic(reviewPublic)
                .preferredCategories(new ArrayList<>())
                .build();
    }

    private User buildReviewer() {
        return User.builder()
                .id(2L)
                .nickname("승우")
                .profileImageObjectKey("profiles/user2.png")
                .build();
    }

    private Page<Review> buildReviewPage(User target, User reviewer) {
        Review review1 =
                Review.builder()
                        .id(10L)
                        .reviewer(reviewer)
                        .reviewee(target)
                        .gathering(Gathering.builder().id(100L).build())
                        .rating(5)
                        .content("좋았어요")
                        .createdAt(LocalDateTime.now().minusDays(1))
                        .updatedAt(LocalDateTime.now().minusDays(1))
                        .build();
        Review review2 =
                Review.builder()
                        .id(9L)
                        .reviewer(reviewer)
                        .reviewee(target)
                        .gathering(Gathering.builder().id(100L).build())
                        .rating(4)
                        .content("괜찮았어요")
                        .createdAt(LocalDateTime.now().minusDays(2))
                        .updatedAt(LocalDateTime.now().minusDays(2))
                        .build();
        Review review3 =
                Review.builder()
                        .id(8L)
                        .reviewer(reviewer)
                        .reviewee(target)
                        .gathering(Gathering.builder().id(100L).build())
                        .rating(3)
                        .content("그냥 그래요")
                        .createdAt(LocalDateTime.now().minusDays(3))
                        .updatedAt(LocalDateTime.now().minusDays(3))
                        .build();
        return new PageImpl<>(List.of(review1, review2, review3));
    }

    private void stubReviewVisible(Long userId, User reviewer, Page<Review> reviewPage) {
        when(fileUrlResolver.toPublicUrl("profiles/user2.png"))
                .thenReturn("https://cdn.example.com/profiles/user2.png");
        when(reviewRepository.findByRevieweeId(eq(userId), any(Pageable.class)))
                .thenReturn(reviewPage);
        when(reviewRepository.findAverageRatingByRevieweeId(userId)).thenReturn(4.0);
    }

    // ──────────────────────────────────────────────
    // 성공 케이스
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("프로필 조회 (200 OK): reviewPublic=true인 유저를 타인이 조회하면 리뷰 프리뷰 반환")
    void getUserProfile_success_publicReview_byOtherUser() {
        // given
        Long userId = 1L;
        Long currentUserId = 99L; // 타인

        User user = buildTargetUser(userId, true);
        User reviewer = buildReviewer();
        Page<Review> reviewPage = buildReviewPage(user, reviewer);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileUrlResolver.toPublicUrl("profiles/user1.png"))
                .thenReturn("https://cdn.example.com/profiles/user1.png");
        when(reviewRepository.countByRevieweeId(userId)).thenReturn(3L);
        stubReviewVisible(userId, reviewer, reviewPage);
        when(reviewRepository.findAverageRatingByRevieweeId(userId)).thenReturn(4.0);

        // when
        var result = userService.getUserProfile(userId, currentUserId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("usr_1");
        assertThat(result.getNickname()).isEqualTo("진지충");
        assertThat(result.getProfileImageUrl())
                .isEqualTo("https://cdn.example.com/profiles/user1.png");
        assertThat(result.getReviewPublic()).isTrue();

        assertThat(result.getReviewsPreview()).isNotNull();
        assertThat(result.getReviewsPreview().data()).hasSize(3);
        assertThat(result.getReviewsPreview().data().get(0).reviewerNickname()).isEqualTo("승우");
        assertThat(result.getReviewsPreview().meta().sortedBy()).isNotBlank();

        verify(userRepository, times(1)).findById(userId);
        verify(reviewRepository, times(1)).findByRevieweeId(eq(userId), any(Pageable.class));
        verify(reviewRepository, times(1)).findAverageRatingByRevieweeId(userId);
        verify(reviewRepository, times(1)).countByRevieweeId(userId);
        verify(fileUrlResolver, atLeastOnce()).toPublicUrl(anyString());
        verifyNoMoreInteractions(userRepository, reviewRepository, fileUrlResolver);
    }

    @Test
    @DisplayName("프로필 조회 (200 OK): reviewPublic=false여도 본인이 조회하면 리뷰 프리뷰 반환")
    void getUserProfile_success_privateReview_byOwner() {
        // given
        Long userId = 1L;
        Long currentUserId = 1L; // 본인

        User user = buildTargetUser(userId, false); // reviewPublic=false
        User reviewer = buildReviewer();
        Page<Review> reviewPage = buildReviewPage(user, reviewer);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileUrlResolver.toPublicUrl("profiles/user1.png"))
                .thenReturn("https://cdn.example.com/profiles/user1.png");
        when(reviewRepository.countByRevieweeId(userId)).thenReturn(3L);
        stubReviewVisible(userId, reviewer, reviewPage);
        when(reviewRepository.findAverageRatingByRevieweeId(userId)).thenReturn(4.0);

        // when
        var result = userService.getUserProfile(userId, currentUserId);

        // then
        assertThat(result.getReviewPublic()).isFalse();
        assertThat(result.getReviewsPreview()).isNotNull();
        assertThat(result.getReviewsPreview().data()).hasSize(3);

        verify(userRepository, times(1)).findById(userId);
        verify(reviewRepository, times(1)).findByRevieweeId(eq(userId), any(Pageable.class));
        verify(reviewRepository, times(1)).findAverageRatingByRevieweeId(userId);
        verify(reviewRepository, times(1)).countByRevieweeId(userId);
        verify(fileUrlResolver, atLeastOnce()).toPublicUrl(anyString());
        verifyNoMoreInteractions(userRepository, reviewRepository, fileUrlResolver);
    }

    @Test
    @DisplayName("프로필 조회 (200 OK): reviewPublic=false인 유저를 타인이 조회하면 리뷰 프리뷰 null 반환")
    void getUserProfile_success_privateReview_byOtherUser() {
        // given
        Long userId = 1L;
        Long currentUserId = 99L; // 타인

        User user = buildTargetUser(userId, false); // reviewPublic=false

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileUrlResolver.toPublicUrl("profiles/user1.png"))
                .thenReturn("https://cdn.example.com/profiles/user1.png");
        when(reviewRepository.countByRevieweeId(userId)).thenReturn(3L);

        // when
        var result = userService.getUserProfile(userId, currentUserId);

        // then
        assertThat(result.getReviewPublic()).isFalse();
        assertThat(result.getReviewsPreview()).isNull();
        assertThat(result.getAverageRating()).isNull();

        verify(userRepository, times(1)).findById(userId);
        verify(reviewRepository, times(1)).countByRevieweeId(userId);
        // 리뷰 비공개이므로 리뷰 조회 쿼리는 호출되지 않아야 함
        verify(reviewRepository, never()).findByRevieweeId(any(), any(Pageable.class));
        verify(reviewRepository, never()).findAverageRatingByRevieweeId(any());
        verifyNoMoreInteractions(userRepository, reviewRepository, fileUrlResolver);
    }

    // ──────────────────────────────────────────────
    // 실패 케이스
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("프로필 조회 (404 Not Found): 대상 유저가 없으면 USER_NOT_FOUND 예외")
    void getUserProfile_userNotFound() {
        // given
        Long userId = 999L;
        Long currentUserId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserProfile(userId, currentUserId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        verify(userRepository, times(1)).findById(userId);
        verifyNoInteractions(reviewRepository, fileUrlResolver);
        verifyNoMoreInteractions(userRepository);
    }
}
