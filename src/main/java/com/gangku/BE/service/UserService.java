// src/main/java/com/gangku/BE/service/UserService.java

package com.gangku.BE.service;

import com.gangku.BE.domain.User;
import com.gangku.BE.dto.SignupRequestDto;
import com.gangku.BE.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// 서비스 계층임을 나타내는 어노테이션 → 스프링이 자동으로 빈으로 등록함
@Service
@RequiredArgsConstructor // final 필드를 자동으로 생성자 주입해줌
public class UserService {

    // UserRepository를 의존성 주입 (생성자 방식)
    private final UserRepository userRepository;

    // 회원가입 메서드 - 트랜잭션이 필요한 비즈니스 로직
    @Transactional
    public User registerUser(SignupRequestDto requestDto) {

        // 닉네임 중복 여부 확인
        if (userRepository.existsByNickname(requestDto.getNickname())) {
            // 중복이면 예외 던지기 (예외 클래스는 나중에 따로 정의하자)
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 2. 프로필 이미지의 URL을 직접 조합 (bucket + key)
        String profileImageUrl = "https://cdn.example.com/"
                + requestDto.getProfileImage().getKey(); // 실제 구현에서는 CDN 구조 반영

        // 3. User 엔티티 생성
        User user = User.builder()
                .userId(UUID.randomUUID().toString())
                .email(requestDto.getEmail())
                .password(requestDto.getPassword()) // 나중에 암호화 필요
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

}