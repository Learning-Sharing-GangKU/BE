// src/main/java/com/gangku/BE/dto/SignupResponseDto.java

package com.gangku.be.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gangku.be.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
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


    /**
     * User 엔티티와 선호 카테고리 목록을 기반으로 응답 DTO를 생성하는 정적 메서드
     */
    public static SignupResponseDto from(User user, List<String> preferredCategoryNames) {
        return new SignupResponseDto(
                "usr_" + user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getAge(),
                user.getGender(),
                user.getEnrollNumber(),
                preferredCategoryNames,
                user.getCreatedAt()
        );
    }
}