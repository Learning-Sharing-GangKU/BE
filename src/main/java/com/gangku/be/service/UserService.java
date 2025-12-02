package com.gangku.be.service;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.PreferredCategory;
import com.gangku.be.dto.user.SignUpRequestDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.PreferredCategoryRepository;
import java.util.List;
import java.util.regex.Pattern;
import com.gangku.be.domain.User;
import com.gangku.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

    private final PasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PASSWORD_REGEX = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    public User registerUser(SignUpRequestDto signUpRequestDto) {

        // 이메일 형식 에러 예외처리
        validateEmailFormat(signUpRequestDto.getEmail());

        // 비밀번호 규칙 에러 예외처리
        validatePasswordWeakness(signUpRequestDto.getPassword());

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
        User newUser = User.create(
                signUpRequestDto.getEmail(),
                passwordEncoder.encode(signUpRequestDto.getPassword()),
                signUpRequestDto.getNickname(),
                signUpRequestDto.getAge(),
                signUpRequestDto.getGender(),
                signUpRequestDto.getEnrollNumber(),
                signUpRequestDto.getProfileImage()
        );

        assignPreferredCategories(signUpRequestDto.getPreferredCategories(), newUser);

        return userRepository.save(newUser);
    }

    /**
     * --- 검증 및 반환 헬퍼 메서드 ---
     */
    
    private void validateEmailFormat(String email) {
        if (email != null && !EMAIL_REGEX.matcher(email).matches()) {
            throw new CustomException(UserErrorCode.INVALID_EMAIL_FORMAT);
        }
    }

    private void validatePasswordWeakness(String password) {
        if (password != null && !PASSWORD_REGEX.matcher(password).matches()) {
            throw new CustomException(UserErrorCode.PASSWORD_TOO_WEAK);
        }
    }
    
    private void validateEmailConflict(String email) {
        if(userRepository.existsByEmail(email)) {
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

        List<PreferredCategory> preferredCategoryList = categories.stream()
                .map(category -> {
                    PreferredCategory preferredCategory = new PreferredCategory();
                    preferredCategory.setCategory(category);

                    newUser.addPreferredCategory(preferredCategory);

                    return preferredCategory;
                })
                .toList();

        preferredCategoryRepository.saveAll(preferredCategoryList);
    }
}
