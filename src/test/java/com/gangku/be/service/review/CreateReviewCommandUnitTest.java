package com.gangku.be.service.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Review;
import com.gangku.be.domain.User;
import com.gangku.be.dto.review.ReviewCreateRequestDto;
import com.gangku.be.dto.review.ReviewCreateResponseDto;
import com.gangku.be.repository.ReviewRepository;
import com.gangku.be.service.command.ReviewCommandService;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class CreateReviewCommandUnitTest {

    @Mock private ReviewRepository reviewRepository;

    @InjectMocks private ReviewCommandService reviewCommandService;

    @Test
    @DisplayName("리뷰 저장 성공: 유효한 reviewer, reviewee, gathering이면 리뷰 저장 성공")
    void saveReview_success() throws Exception {
        // given
        Long reviewerId = 1L;
        Long revieweeId = 2L;
        Long gatheringId = 10L;

        User reviewer = User.builder().id(reviewerId).build();
        User reviewee = User.builder().id(revieweeId).build();
        Gathering gathering = Gathering.builder().id(gatheringId).build();

        ReviewCreateRequestDto requestDto = new ReviewCreateRequestDto(4, "좋았어요!");

        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(
                        inv -> {
                            Review r = inv.getArgument(0);
                            Field f = Review.class.getDeclaredField("id");
                            f.setAccessible(true);
                            f.set(r, 1L);
                            return r;
                        });

        // when
        ReviewCreateResponseDto response =
                reviewCommandService.saveReview(reviewer, reviewee, gathering, requestDto);

        // then
        assertThat(response).isNotNull();

        verify(reviewRepository, times(1)).save(any(Review.class));
        verifyNoMoreInteractions(reviewRepository);
    }
}
