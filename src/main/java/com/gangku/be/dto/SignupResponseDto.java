// src/main/java/com/gangku/BE/dto/SignupResponseDto.java

package com.gangku.be.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gangku.be.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class SignupResponseDto {

    @JsonProperty
    private final String id;
    private final String email;
    private final String nickname;
    private final String profileImageUrl;
    private final int age;
    private final String gender;
    private final int enrollNumber;
    private final List<String> preferredCategories;
    private final LocalDateTime createdAt;

    @Builder
    public SignupResponseDto(String id, String email, String nickname,
                             String profileImageUrl, int age, String gender,
                             int enrollNumber, List<String> preferredCategories,
                             LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.age = age;
        this.gender = gender;
        this.enrollNumber = enrollNumber;
        this.preferredCategories = preferredCategories;
        this.createdAt = createdAt;
    }

    // User → SignupResponseDto 로 변환하는 static 메서드
    public static SignupResponseDto from(User user) {
        return SignupResponseDto.builder()
                .id("usr_" + user.getUserId()) //Long → String 변환
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl()) // getter 이름 주의
                .age(user.getAge())
                .gender(user.getGender())
                .enrollNumber(user.getEnrollNumber())
                .preferredCategories(user.getPreferredCategories())
                .createdAt(user.getCreatedAt())
                .build();
    }
}