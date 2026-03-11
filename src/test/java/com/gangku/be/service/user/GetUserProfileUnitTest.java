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

    @Test
    @DisplayName("프로필 조회 (200 OK): 유저 정보 + 선호카테고리 + 리뷰 프리뷰 반환")
    void getUserProfile_success() {
        // given
        Long userId = 1L;

        User user =
                User.builder()
                        .id(userId)
                        .nickname("진지충")
                        .age(23)
                        .gender("MALE")
                        .enrollNumber(22)
                        .profileImageObjectKey("profiles/user1.png")
                        .reviewsPublic(true)
                        .preferredCategories(new ArrayList<>())
                        .build();

        // 리뷰어 유저
        User reviewer =
                User.builder()
                        .id(2L)
                        .nickname("승우")
                        .profileImageObjectKey("profiles/user2.png")
                        .build();

        // Review 엔티티는 다른 팀원이 만들었다고 했지만, 현재 공유된 엔티티 기준으로 빌더 사용
        Review review1 =
                Review.builder()
                        .id(10L)
                        .reviewer(reviewer)
                        .reviewee(user)
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
                        .reviewee(user)
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
                        .reviewee(user)
                        .gathering(Gathering.builder().id(100L).build())
                        .rating(3)
                        .content("그냥 그래요")
                        .createdAt(LocalDateTime.now().minusDays(3))
                        .updatedAt(LocalDateTime.now().minusDays(3))
                        .build();

        // 선호카테고리는 User.preferredCategories를 통해 내려가므로 여기서는 0개로 둔다
        // (선호카테고리 mapping은 도메인 구조에 따라 별도 fixture로 채워도 됨)

        Page<Review> reviewPage = new PageImpl<>(List.of(review1, review2, review3));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(fileUrlResolver.toPublicUrl("profiles/user1.png"))
                .thenReturn("https://cdn.example.com/profiles/user1.png");
        when(fileUrlResolver.toPublicUrl("profiles/user2.png"))
                .thenReturn("https://cdn.example.com/profiles/user2.png");

        when(reviewRepository.findByRevieweeId(eq(userId), any(Pageable.class)))
                .thenReturn(reviewPage);

        // when
        var result = userService.getUserProfile(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getNickname()).isEqualTo("진지충");
        assertThat(result.getProfileImageUrl())
                .isEqualTo("https://cdn.example.com/profiles/user1.png");
        assertThat(result.getReviewsPublic()).isTrue();

        // reviewsPreview 검증 (data/meta 구조만 간단 검증)
        assertThat(result.getReviewsPreview()).isNotNull();
        assertThat(result.getReviewsPreview().data()).hasSize(3);
        assertThat(result.getReviewsPreview().data().get(0).reviewerNickname()).isEqualTo("승우");
        assertThat(result.getReviewsPreview().meta().sortedBy()).isNotBlank();

        verify(userRepository, times(1)).findById(userId);
        verify(reviewRepository, times(1)).findByRevieweeId(eq(userId), any(Pageable.class));
        verify(fileUrlResolver, atLeastOnce()).toPublicUrl(anyString());
        verifyNoMoreInteractions(userRepository, reviewRepository, fileUrlResolver);
    }

    @Test
    @DisplayName("프로필 조회 (404 Not Found): 대상 유저가 없으면 USER_NOT_FOUND 예외")
    void getUserProfile_userNotFound() {
        // given
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when
        assertThatThrownBy(() -> userService.getUserProfile(userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        // then
        verify(userRepository, times(1)).findById(userId);
        verifyNoInteractions(reviewRepository, fileUrlResolver);
        verifyNoMoreInteractions(userRepository);
    }
}
