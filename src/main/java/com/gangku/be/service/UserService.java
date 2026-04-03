package com.gangku.be.service;

import com.gangku.be.constant.user.UserReviewSort;
import com.gangku.be.domain.*;
import com.gangku.be.domain.Category;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.PreferredCategory;
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
import com.gangku.be.model.review.ReviewCursor;
import com.gangku.be.model.review.ReviewCursorCodec;
import com.gangku.be.model.review.ReviewPageables;
import com.gangku.be.model.review.ReviewsPreview;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.PreferredCategoryRepository;
import com.gangku.be.repository.ReviewRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.util.ai.AiTextFilterMapper;
import com.gangku.be.util.object.FileUrlResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PreferredCategoryRepository preferredCategoryRepository;

    private final FileUrlResolver fileUrlResolver;
    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ReviewRepository reviewRepository;

    private final AiApiClient aiApiClient;
    private final AiTextFilterMapper aiTextFilterMapper;

    public User registerUser(SignUpRequestDto signUpRequestDto, String sessionId) {

        validateEmailVerification(sessionId, signUpRequestDto.getEmail());

        // 중복된 이메일 예외처리
        validateEmailConflict(signUpRequestDto.getEmail());

        // 중복된 닉네임 예외처리
        validateNicknameConflict(signUpRequestDto.getNickname());

        validateNicknameAllowedFromSignUp(signUpRequestDto);

        // 4) DB에 저장
        User newUser =
                User.create(
                        signUpRequestDto.getEmail(),
                        passwordEncoder.encode(signUpRequestDto.getPassword()),
                        signUpRequestDto.getNickname(),
                        signUpRequestDto.getAge(),
                        signUpRequestDto.getGender(),
                        signUpRequestDto.getEnrollNumber(),
                        signUpRequestDto.getProfileImageObjectKey());

        userRepository.save(newUser);

        stringRedisTemplate.delete("auth:signup:session:" + sessionId);

        if (signUpRequestDto.getPreferredCategories() != null) {
            assignPreferredCategories(signUpRequestDto.getPreferredCategories(), newUser);
        }

        return newUser;
    }

    public void deleteUser(Long targetUserId, Long currentUserId) {

        User user = findUserById(targetUserId);

        validateUserPrincipal(currentUserId, user);

        List<Participation> participations = new ArrayList<>(user.getParticipations());

        for (Participation participation : participations) {
            participation.withdraw();
        }

        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponseDto getUserProfile(Long userId) {

        User user = findUserById(userId);
        String profileImageUrl = resolveImageUrl(user.getProfileImageObjectKey());

        List<String> preferredCategories =
                user.getPreferredCategories().stream()
                        .map(pc -> pc.getCategory().getName())
                        .toList();

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
        Long reviewCount = reviewRepository.countByRevieweeId(userId);

        return UserProfileResponseDto.from(
                user, profileImageUrl, preferredCategories, roundedAverageRating, reviewCount, reviewsPreview);
    }

    @Transactional
    public UserProfileUpdateResponseDto updateUserProfile(
            Long targetUserId, Long currentUserId, UserProfileUpdateRequestDto requestDto) {

        User user = findUserById(targetUserId);

        validateUserProfileOwner(currentUserId, user);

        validateNickNameAllowedFromProfileUpdate(requestDto);

        updateProfileFields(user, requestDto);

        if (requestDto.getPreferredCategories() != null) {
            replacePreferredCategories(user, requestDto.getPreferredCategories());
        }

        User savedUser = userRepository.save(user);

        String profileImageUrl = resolveImageUrl(savedUser.getProfileImageObjectKey());

        List<String> preferredCategories =
                savedUser.getPreferredCategories().stream()
                        .map(pc -> pc.getCategory().getName())
                        .toList();

        return UserProfileUpdateResponseDto.from(savedUser, profileImageUrl, preferredCategories);
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

    private void updateProfileFields(User user, UserProfileUpdateRequestDto requestDto) {
        if (requestDto.getNickname() != null
                && userRepository.existsByNicknameAndIdNot(
                        requestDto.getNickname(), user.getId())) {
            throw new CustomException(UserErrorCode.NICKNAME_ALREADY_EXISTS);
        }
        user.updateProfile(
                requestDto.getProfileImageObjectKey(),
                requestDto.getNickname(),
                requestDto.getAge(),
                requestDto.getGender(),
                requestDto.getEnrollNumber());
    }

    // 반올림 메서드
    private Double roundToOneDecimalPlace(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 10) / 10.0;
    }

    private void replacePreferredCategories(User user, List<String> preferredCategories) {
        user.getPreferredCategories().clear();
        userRepository.flush();
        assignPreferredCategories(preferredCategories, user);
    }

    /** --- 검증 및 반환 헬퍼 메서드 --- */
    private void validateEmailVerification(String sessionId, String email) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }

        String sessionKey = "auth:signup:session:" + sessionId;
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

    private void assignPreferredCategories(List<String> preferredCategories, User newUser) {

        if (preferredCategories != null && preferredCategories.isEmpty()) {
            return;
        }

        List<String> distinctCategories = preferredCategories.stream().distinct().toList();

        List<Category> categories = categoryRepository.findByNameIn(distinctCategories);

        List<PreferredCategory> preferredCategoryList =
                categories.stream()
                        .map(
                                category -> {
                                    PreferredCategory preferredCategory = new PreferredCategory();
                                    preferredCategory.assignCategory(category);

                                    newUser.addPreferredCategory(preferredCategory);

                                    return preferredCategory;
                                })
                        .toList();

        preferredCategoryRepository.saveAll(preferredCategoryList);
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

    private void validateUserProfileOwner(Long currentUserId, User user) {
        if (!user.getId().equals(currentUserId)) {
            throw new CustomException(UserErrorCode.NO_PERMISSION_TO_UPDATE_PROFILE);
        }
    }

    private void validateReviewVisibility(Long currentUserId, User targetUser) {
        boolean isOwner = targetUser.getId().equals(currentUserId);
        boolean isPublic = Boolean.TRUE.equals(targetUser.getReviewPublic());

        if (!isOwner && !isPublic) {
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

    private void validateNicknameAllowedFromSignUp(SignUpRequestDto signUpRequestDto) {
        TextFilterRequestDto textFilterRequestDto = aiTextFilterMapper.fromSignUp(signUpRequestDto);
        TextFilterResponseDto textFilterResponseDto = aiApiClient.filterText(textFilterRequestDto);

        if (!textFilterResponseDto.isAllowed()) {
            throw new CustomException(UserErrorCode.INVALID_NICKNAME);
        }
    }
}
