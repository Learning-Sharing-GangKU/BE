package com.gangku.be.service;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.PreferredCategory;
import com.gangku.be.domain.User;
import com.gangku.be.dto.user.SignUpRequestDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.AuthErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.PreferredCategoryRepository;
import com.gangku.be.repository.UserRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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

    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(SignUpRequestDto signUpRequestDto, String sessionId) {

        validateEmailVerification(sessionId, signUpRequestDto.getEmail());

        // 중복된 이메일 예외처리
        validateEmailConflict(signUpRequestDto.getEmail());

        // 중복된 닉네임 예외처리
        validateNicknameConflict(signUpRequestDto.getNickname());

        /*
        여기서 회원가입 DB로 처리 하기 전에
        request = {
            scenario = "nickname"
            text = signUpRequestDto.getNickname()
        }
        으로 (POST)http://127.0.0.1:8000/api/ai/v1/text/filter으로 보내줘여됨(url 주소 확인 바람.)

        유저 프로필 수정 없다고 하셨으니깐(제가 잘 모르고 있는 걸 수도 있음)
        따로 주석처리 안 할게요
         */

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

        assignPreferredCategories(signUpRequestDto.getPreferredCategories(), newUser);

        return newUser;
    }

    @Transactional
    public void deleteUser(Long targetUserId, Long currentUserId) {

        User user = findUserById(targetUserId);

        validateUserPrincipal(currentUserId, user);

        userRepository.delete(user);
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

        if (!"1".equals(verified) || !email.equals(sessionEmail)) {
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
                                    preferredCategory.setCategory(category);

                                    newUser.addPreferredCategory(preferredCategory);

                                    return preferredCategory;
                                })
                        .toList();

        preferredCategoryRepository.saveAll(preferredCategoryList);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    private void validateUserPrincipal(Long currentUserId, User user) {
        if (!user.getId().equals(currentUserId)) {
            throw new CustomException(UserErrorCode.NO_PERMISSION_TO_CANCEL_MEMBERSHIP);
        }
    }
}
