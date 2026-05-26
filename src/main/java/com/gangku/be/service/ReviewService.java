package com.gangku.be.service;

import com.gangku.be.domain.Gathering;
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
import com.gangku.be.service.command.ReviewCommandService;
import com.gangku.be.util.ai.AiTextFilterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final GatheringRepository gatheringRepository;
    private final ParticipationRepository participationRepository;
    private final AiApiClient aiApiClient;
    private final AiTextFilterMapper aiTextFilterMapper;
    private final ReviewCommandService reviewCommandService; // 추가

    public ReviewCreateResponseDto createReview(
            Long reviewerId, Long revieweeId, ReviewCreateRequestDto reviewCreateRequestDto) {

        validateDifferentUser(reviewerId, revieweeId);

        User reviewer = findUserById(reviewerId);
        User reviewee = findUserById(revieweeId);
        Long gatheringId = findGatheringIdParticipatedTogether(reviewerId, revieweeId);
        Gathering gathering = findGatheringById(gatheringId);
        validateNotDuplicatedReview(gatheringId, reviewerId, revieweeId);

        validateReviewCommentAllowed(reviewCreateRequestDto);

        return reviewCommandService.saveReview(
                reviewer, reviewee, gathering, reviewCreateRequestDto);
    }

    private Gathering findGatheringById(Long gatheringId) {
        return gatheringRepository
                .findById(gatheringId)
                .orElseThrow(() -> new CustomException(GatheringErrorCode.GATHERING_NOT_FOUND));
    }

    private Long findGatheringIdParticipatedTogether(Long reviewerId, Long revieweeId) {
        return participationRepository
                .findFinishedCommonGatheringIds(reviewerId, revieweeId)
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> new CustomException(ReviewErrorCode.NO_PERMISSION_TO_WRITE_REVIEW));
    }

    private void validateNotDuplicatedReview(Long gatheringId, Long reviewerId, Long revieweeId) {
        if (reviewRepository.existsByGatheringIdAndReviewerIdAndRevieweeId(
                gatheringId, reviewerId, revieweeId)) {
            throw new CustomException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }
    }

    private User findUserById(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    private void validateDifferentUser(Long reviewerId, Long revieweeId) {
        if (reviewerId.equals(revieweeId)) {
            throw new CustomException(ReviewErrorCode.INVALID_REVIEW_TARGET);
        }
    }

    private void validateReviewCommentAllowed(ReviewCreateRequestDto reviewCreateRequestDto) {
        TextFilterRequestDto textFilterRequestDto =
                aiTextFilterMapper.fromReviewCreate(reviewCreateRequestDto);
        TextFilterResponseDto textFilterResponseDto = aiApiClient.filterText(textFilterRequestDto);

        if (!textFilterResponseDto.isAllowed()) {
            throw new CustomException(ReviewErrorCode.INVALID_REVIEW_COMMENT);
        }
    }
}
