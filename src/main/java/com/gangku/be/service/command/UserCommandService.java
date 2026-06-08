package com.gangku.be.service.command;

import com.gangku.be.constant.auth.RedisKeys;
import com.gangku.be.domain.*;
import com.gangku.be.dto.user.SignUpRequestDto;
import com.gangku.be.dto.user.UserProfileUpdateRequestDto;
import com.gangku.be.dto.user.UserProfileUpdateResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.repository.*;
import com.gangku.be.util.object.FileUrlResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCommandService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PreferredCategoryRepository preferredCategoryRepository;
    private final FileUrlResolver fileUrlResolver;
    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User saveUser(SignUpRequestDto signUpRequestDto, String sessionId) {

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

        stringRedisTemplate.delete(RedisKeys.signupSessionKey(sessionId));

        if (signUpRequestDto.getPreferredCategories() != null) {
            assignPreferredCategories(signUpRequestDto.getPreferredCategories(), newUser);
        }

        return newUser;
    }

    @Transactional
    public UserProfileUpdateResponseDto updateUserProfile(
            Long targetUserId, Long currentUserId, UserProfileUpdateRequestDto requestDto) {

        User user = findUserById(targetUserId);

        validateUserProfileOwner(currentUserId, user);
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

    private void replacePreferredCategories(User user, List<String> preferredCategories) {
        user.getPreferredCategories().clear();
        userRepository.flush();
        assignPreferredCategories(preferredCategories, user);
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

    private String resolveImageUrl(String key) {
        if (key == null || key.isBlank()) return null;
        return fileUrlResolver.toPublicUrl(key);
    }

    private User findUserById(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    private void validateUserProfileOwner(Long currentUserId, User user) {
        if (!user.getId().equals(currentUserId)) {
            throw new CustomException(UserErrorCode.NO_PERMISSION_TO_UPDATE_PROFILE);
        }
    }
}
