

package com.gangku.be.service;

import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import com.gangku.be.domain.User;
import com.gangku.be.dto.user.LoginRequestDto;
import com.gangku.be.dto.user.LoginResponseDto;
import com.gangku.be.dto.user.SignupRequestDto;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.gangku.be.util.ValidationUtil.*;
import java.time.LocalDateTime;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PreferredCategoryService preferredCategoryService;


    // 유저ID 조회 메서드
    public User findByUserId(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public void save(User user) { //저장만 하고 반환값 쓰이지 않으므로 void
        userRepository.save(user);
    }

    // 회원가입 메서드
    public User registerUser(SignupRequestDto requestDto) {
        log.info("✅ 회원가입 시작: 이메일={}, 닉네임={}", requestDto.getEmail(), requestDto.getNickname());


        // 2. 프로필 이미지의 URL을 직접 조합 (bucket + key)
        // NullPointerException 가능성 존재, 추후 수정해야함.
        String profileImageUrl = "https://cdn.example.com/"
                + requestDto.getProfileImage().getKey(); // 실제 구현에서는 CDN 구조 반영

        // 이메일 형식 에러 예외처리
        if (!isValidEmail(requestDto.getEmail())) {
            throw new CustomException(ErrorCode.INVALID_EMAIL_FORMAT);
        }

        // 비밀번호 규칙 에러 예외처리
        if (!isValidPassword(requestDto.getPassword())) {
            throw new CustomException(ErrorCode.PASSWORD_TOO_WEAK);
        }

        // 중복된 이메일 예외처리
        if(userRepository.existsByEmail(requestDto.getEmail())){
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 중복된 닉네임 예외처리
        if (userRepository.existsByNickname(requestDto.getNickname())) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        // 3. User 엔티티 생성
        User user = User.builder()
                .email(requestDto.getEmail())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .nickname(requestDto.getNickname())
                .age(requestDto.getAge())
                .gender(requestDto.getGender())
                .enrollNumber(requestDto.getEnrollNumber())
                .photoUrl(profileImageUrl)
                .emailVerified(false)
                .reviewsPublic(true)
                .createdAt(null)     // @PrePersist로 자동 설정됨
                .updatedAt(null)     // @PrePersist/@PreUpdate로 자동 설정됨
                .build();
        log.info("🛠️ User 엔티티 빌드 완료: {}", user);
        User savedUser = userRepository.save(user);
        preferredCategoryService.setPreferredCategories(savedUser, requestDto.getPreferredCategories());
        log.info("✅ 사용자 저장 완료: ID={}, 닉네임={}", user.getId(), user.getNickname());
        return savedUser;
        // 4. DB에 저장

    }

    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    // 로그인 (비밀번호 체크)
    public User authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return user;
    }

    // 로그인 → JWT 토큰 생성
    public LoginResponseDto login(LoginRequestDto dto) {
        User user = authenticate(dto.getEmail(), dto.getPassword());
        String accessToken = jwtTokenProvider.generateAccessToken(String.valueOf(user.getId()));
        String refreshToken = jwtTokenProvider.generateRefreshToken(String.valueOf(user.getId()));

        user.setRefreshToken(refreshToken);
        user.setRefreshExpiry(LocalDateTime.now().plusDays(7)); // 리프레시 토큰 만료일
        userRepository.save(user);

        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValidity()) // 초 단위
                .build();
    }


}

