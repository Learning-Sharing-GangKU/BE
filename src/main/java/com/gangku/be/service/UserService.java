package com.gangku.be.service;

import com.gangku.be.constant.auth.RedisKeys;
import com.gangku.be.constant.user.UserReviewSort;
import com.gangku.be.domain.*;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import com.gangku.be.dto.ai.request.TextFilterRequestDto;
import com.gangku.be.dto.ai.response.TextFilterResponseDto;
import com.gangku.be.dto.review.ReviewListResponseDto;
import com.gangku.be.dto.user.SignUpRequestDto;
import com.gangku.be.dto.user.UpdateReviewSettingResponseDto;
import com.gangku.be.dto.user.UserProfileResponseDto;
import com.gangku.be.dto.user.UserProfileUpdateRequestDto;
import com.gangku.be.dto.user.UserProfileUpdateResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.AuthErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.external.ai.AiApiClient;
import com.gangku.be.external.ai.AiResponses;
import com.gangku.be.model.review.ReviewCursor;
import com.gangku.be.model.review.ReviewCursorCodec;
import com.gangku.be.model.review.ReviewPageables;
import com.gangku.be.model.review.ReviewsPreview;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.ReviewRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.command.UserCommandService;
import com.gangku.be.util.ai.AiTextFilterMapper;
import com.gangku.be.util.object.FileUrlResolver;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;

    private final FileUrlResolver fileUrlResolver;
    private final StringRedisTemplate stringRedisTemplate;
    private final ReviewRepository reviewRepository;

    private final AiApiClient aiApiClient;
    private final AiTextFilterMapper aiTextFilterMapper;

    private final UserCommandService userCommandService;

    public User registerUser(SignUpRequestDto signUpRequestDto, String sessionId) {

        // in-memory 가드: sessionId null/blank면 AI 호출 전에 즉시 차단
        if (sessionId == null || sessionId.isBlank()) {
            throw new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }

        // AI 검증을 aiTaskExecutor 스레드에서 비동기 시작 (Redis/DB 검증과 병렬 실행)
        TextFilterRequestDto filterReq = aiTextFilterMapper.fromSignUp(signUpRequestDto);
        CompletableFuture<TextFilterResponseDto> aiFuture = aiApiClient.filterTextAsync(filterReq);

        // Redis/DB 검증 (AI 호출과 병렬로 실행됨)
        validateEmailVerification(sessionId, signUpRequestDto.getEmail());
        validateEmailConflict(signUpRequestDto.getEmail());
        validateNicknameConflict(signUpRequestDto.getNickname());

        // AI 결과 수신 (검증 완료 후 await, 이미 완료됐을 가능성 높음)
        TextFilterResponseDto filterResult = AiResponses.await(aiFuture);
        if (!filterResult.isAllowed()) {
            throw new CustomException(UserErrorCode.INVALID_NICKNAME);
        }

        return userCommandService.saveUser(signUpRequestDto, sessionId);
    }

    @Transactional
    public void deleteUser(Long targetUserId, Long currentUserId) {

        User user = findUserById(targetUserId);

        validateUserPrincipal(currentUserId, user);

        List<Participation> participations =
                participationRepository.findAllByUserWithGatheringAndHost(user);

        for (Participation participation : participations) {
            Gathering gathering = participation.getGathering();

            if (!gathering.getHost().getId().equals(user.getId())) {
                gathering.decreaseParticipantCount();
            }
        }

        participationRepository.deleteAll(participations);

        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponseDto getUserProfile(Long userId, Long currentUserId) {

        User user = findUserById(userId);
        String profileImageUrl = resolveImageUrl(user.getProfileImageObjectKey());
        List<String> preferredCategories =
                user.getPreferredCategories().stream()
                        .map(pc -> pc.getCategory().getName())
                        .toList();

        Long reviewCount = reviewRepository.countByRevieweeId(userId);

        if (!isReviewVisible(currentUserId, user)) {
            return UserProfileResponseDto.from(
                    user, profileImageUrl, preferredCategories, null, reviewCount, null);
        }

        // 정렬 정책 고정
        UserReviewSort reviewSort = UserReviewSort.CREATED_AT_DESC;

        // 리뷰 조회
        Page<Review> reviewPage =
                reviewRepository.findByRevieweeId(userId, ReviewPageables.preview(reviewSort));

        // 리뷰 프리뷰 생성
        ReviewsPreview reviewsPreview =
                ReviewsPreview.from(
                        reviewPage.getContent(),
                        3,
                        reviewSort.toSortedByForSpec(),
                        r -> resolveImageUrl(r.getReviewer().getProfileImageObjectKey()));

        Double averageRating = reviewRepository.findAverageRatingByRevieweeId(userId);
        Double roundedAverageRating = roundToOneDecimalPlace(averageRating);

        return UserProfileResponseDto.from(
                user,
                profileImageUrl,
                preferredCategories,
                roundedAverageRating,
                reviewCount,
                reviewsPreview);
    }

    public UserProfileUpdateResponseDto updateUserProfile(
            Long targetUserId, Long currentUserId, UserProfileUpdateRequestDto requestDto) {

        validateNickNameAllowedFromProfileUpdate(requestDto);

        return userCommandService.updateUserProfile(targetUserId, currentUserId, requestDto);
    }

    @Transactional
    public UpdateReviewSettingResponseDto updateReviewSetting(
            Long targetUserId, Long currentUserId, Boolean reviewSetting) {

        User user = findUserById(targetUserId);

        validateUserPrincipal(currentUserId, user);

        updateUserReviewsPublic(reviewSetting, user);

        User updatedUser = userRepository.save(user);

        return UpdateReviewSettingResponseDto.from(updatedUser);
    }

    @Transactional(readOnly = true)
    public ReviewListResponseDto getUserReviews(
            Long targetUserId, Long currentUserId, int size, String cursor) {

        User user = findUserById(targetUserId);

        validateReviewVisibility(currentUserId, user);

        int fetchSize = size + 1;

        List<Review> fetchedReviews;

        if (cursor == null || cursor.isBlank()) {
            fetchedReviews =
                    reviewRepository.findFirstPageByRevieweeId(
                            targetUserId,
                            PageRequest.of(
                                    0,
                                    fetchSize,
                                    Sort.by(Sort.Direction.DESC, "createdAt")
                                            .and(Sort.by(Sort.Direction.DESC, "id"))));
        } else {
            ReviewCursor decodedCursor = ReviewCursorCodec.decode(cursor);
            fetchedReviews =
                    reviewRepository.findNextPageByRevieweeIdAndCursorDesc(
                            targetUserId,
                            decodedCursor.createdAt(),
                            decodedCursor.id(),
                            PageRequest.of(0, fetchSize));
        }
        ReviewsPreview reviewsPreview =
                ReviewsPreview.from(
                        fetchedReviews,
                        size,
                        "createdAt,desc",
                        r -> resolveImageUrl(r.getReviewer().getProfileImageObjectKey()));

        return ReviewListResponseDto.from(reviewsPreview);
    }

    private String resolveImageUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return fileUrlResolver.toPublicUrl(key);
    }

    // 반올림 메서드
    private Double roundToOneDecimalPlace(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 10) / 10.0;
    }

    /** --- 검증 및 반환 헬퍼 메서드 --- */
    private void validateEmailVerification(String sessionId, String email) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }

        String sessionKey = RedisKeys.signupSessionKey(sessionId);
        Map<Object, Object> sessionData = stringRedisTemplate.opsForHash().entries(sessionKey);

        if (sessionData.isEmpty()) {
            throw new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }

        String verified = (String) sessionData.get("verified");
        String sessionEmail = (String) sessionData.get("email");

        if (!email.equals(sessionEmail)) {
            throw new CustomException(AuthErrorCode.INVALID_EMAIL_VERIFICATION_SESSION);
        }

        if (!"1".equals(verified)) {
            throw new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    private void validateEmailConflict(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    private void validateNicknameConflict(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new CustomException(UserErrorCode.NICKNAME_ALREADY_EXISTS);
        }
    }

    private User findUserById(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    private void validateUserPrincipal(Long currentUserId, User user) {
        if (!user.getId().equals(currentUserId)) {
            throw new CustomException(UserErrorCode.NO_PERMISSION_TO_ACCESS_OTHER_USER_INFORMATION);
        }
    }

    private boolean isReviewVisible(Long currentUserId, User targetUser) {
        boolean isOwner = currentUserId != null && targetUser.getId().equals(currentUserId);
        boolean isPublic = Boolean.TRUE.equals(targetUser.getReviewPublic());
        return isOwner || isPublic;
    }

    private void validateReviewVisibility(Long currentUserId, User targetUser) {
        if (!isReviewVisible(currentUserId, targetUser)) {
            throw new CustomException(UserErrorCode.NO_PERMISSION_TO_VIEW_REVIEW);
        }
    }

    private void updateUserReviewsPublic(Boolean reviewSetting, User user) {
        user.changeReviewPublic(reviewSetting);
    }

    private void validateNickNameAllowedFromProfileUpdate(
            UserProfileUpdateRequestDto userProfileUpdateRequestDto) {
        if (userProfileUpdateRequestDto.getNickname() != null
                && !userProfileUpdateRequestDto.getNickname().isBlank()) {
            TextFilterRequestDto textFilterRequestDto =
                    aiTextFilterMapper.fromProfileUpdate(userProfileUpdateRequestDto);
            TextFilterResponseDto textFilterResponseDto =
                    aiApiClient.filterText(textFilterRequestDto);

            if (!textFilterResponseDto.isAllowed()) {
                throw new CustomException(UserErrorCode.INVALID_NICKNAME);
            }
        }
    }
}
