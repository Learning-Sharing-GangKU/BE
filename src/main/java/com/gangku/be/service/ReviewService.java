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
import com.gangku.be.external.ai.AiResponses;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.ReviewRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.command.ReviewCommandService;
import com.gangku.be.util.ai.AiTextFilterMapper;
import java.util.concurrent.CompletableFuture;
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

        // in-memory 가드: 명백히 잘못된 요청은 AI 호출 전에 차단
        validateDifferentUser(reviewerId, revieweeId);

        // AI 검증을 aiTaskExecutor 스레드에서 비동기 시작 (DB 검증과 병렬 실행)
        TextFilterRequestDto filterReq =
                aiTextFilterMapper.fromReviewCreate(reviewCreateRequestDto);
        CompletableFuture<TextFilterResponseDto> aiFuture = aiApiClient.filterTextAsync(filterReq);

        // DB 검증 (AI 호출과 병렬로 실행됨)
        User reviewer = findUserById(reviewerId);
        User reviewee = findUserById(revieweeId);
        Long gatheringId = findGatheringIdParticipatedTogether(reviewerId, revieweeId);
        Gathering gathering = findGatheringById(gatheringId);
        validateNotDuplicatedReview(gatheringId, reviewerId, revieweeId);

        // AI 결과 수신 (DB 검증 완료 후 await, 이미 완료됐을 가능성 높음)
        TextFilterResponseDto filterResult = AiResponses.await(aiFuture);
        if (!filterResult.isAllowed()) {
            throw new CustomException(ReviewErrorCode.INVALID_REVIEW_COMMENT);
        }

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
                .findLatestFinishedCommonGatheringId(reviewerId, revieweeId)
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

}
