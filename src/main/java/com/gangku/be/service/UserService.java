// src/main/java/com/gangku/BE/service/UserService.java

package com.gangku.be.service;

import com.gangku.be.domain.User;
import com.gangku.be.dto.LoginRequestDto;
import com.gangku.be.dto.LoginResponseDto;
import com.gangku.be.dto.SignupRequestDto;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

// 서비스 계층임을 나타내는 어노테이션 → 스프링이 자동으로 빈으로 등록함
@Service
@RequiredArgsConstructor // final 필드를 자동으로 생성자 주입해줌
@Transactional
public class UserService {

    private final UserRepository userRepository;

    // 유저ID 조회 메서드
    public User findByUserId(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));
    }
    public User save(User user) {
        return userRepository.save(user);
    }

    // 회원가입 메서드
    public User registerUser(SignupRequestDto requestDto) {

        // 닉네임 중복 여부 확인
        if (userRepository.existsByNickname(requestDto.getNickname())) {
            // 중복이면 예외 던지기 (예외 클래스는 나중에 따로 정의하자)
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 2. 프로필 이미지의 URL을 직접 조합 (bucket + key)
        // NullPointerException 가능성 존재, 추후 수정해야함.
        String profileImageUrl = "https://cdn.example.com/"
                + requestDto.getProfileImage().getKey(); // 실제 구현에서는 CDN 구조 반영


        // 3. User 엔티티 생성
        User user = User.builder()
                .userId(UUID.randomUUID().toString())
                .email(requestDto.getEmail())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .nickname(requestDto.getNickname())
                .age(requestDto.getAge())
                .gender(requestDto.getGender())
                .enrollNumber(requestDto.getEnrollNumber())
                .photoUrl(profileImageUrl)
                .preferredCategories(requestDto.getPreferredCategories())
                .emailVerified(false)
                .reviewsPublic(true)
                .createdAt(null)     // @PrePersist로 자동 설정됨
                .updatedAt(null)     // @PrePersist/@PreUpdate로 자동 설정됨
                .build();

        // 4. DB에 저장
        return userRepository.save(user);
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
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

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

