package com.gangku.be.service;

import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Review;
import com.gangku.be.domain.User;
import com.gangku.be.dto.review.ReviewCreateRequestDto;
import com.gangku.be.dto.review.ReviewCreateResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.exception.constant.ReviewErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.ReviewRepository;
import com.gangku.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final GatheringRepository gatheringRepository;
    private final ParticipationRepository participationRepository;

    @Transactional
    public ReviewCreateResponseDto createReview(
            Long reviewerId, Long revieweeId, ReviewCreateRequestDto reviewCreateRequestDto) {

        validateDifferentUser(reviewerId, revieweeId);

        User reviewer = findUserById(reviewerId);

        User reviewee = findUserById(revieweeId);

        /** 같은 모임인지 확인하고 같은 모임이라면 그 모임 객체 생성하는 로직 필요 */
        Long gatheringId = findGatheringIdParticipatedTogether(reviewerId, revieweeId);
        Gathering gathering = findGatheringById(gatheringId);

        validateNotDuplicatedReview(gatheringId, reviewerId, revieweeId);

        Review review =
                Review.create(
                        reviewer,
                        reviewee,
                        gathering,
                        reviewCreateRequestDto.getRating(),
                        reviewCreateRequestDto.getComment());
        reviewRepository.save(review);

        return ReviewCreateResponseDto.from(review);
    }

    private Gathering findGatheringById(Long gatheringId) {
        Gathering gathering =
                gatheringRepository
                        .findById(gatheringId)
                        .orElseThrow(
                                () -> new CustomException(GatheringErrorCode.GATHERING_NOT_FOUND));
        return gathering;
    }

    private Long findGatheringIdParticipatedTogether(Long reviewerId, Long revieweeId) {
        Long gatheringId =
                participationRepository
                        .findFinishedCommonGatheringIds(reviewerId, revieweeId)
                        .stream()
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new CustomException(
                                                ReviewErrorCode.NO_PERMISSION_TO_WRITE_REVIEW));
        return gatheringId;
    }

    private void validateNotDuplicatedReview(Long gatheringId, Long reviewerId, Long revieweeId) {
        if (reviewRepository.existsByGatheringIdAndReviewerIdAndRevieweeId(
                gatheringId, reviewerId, revieweeId)) {
            throw new CustomException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }
    }

    private User findUserById(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        return user;
    }

    private void validateDifferentUser(Long reviewerId, Long revieweeId) {
        if (reviewerId.equals(revieweeId)) {
            throw new CustomException(ReviewErrorCode.INVALID_REVIEW_TARGET);
        }
    }
}
