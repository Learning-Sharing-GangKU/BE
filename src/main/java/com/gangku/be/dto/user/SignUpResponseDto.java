package com.gangku.be.dto.user;

import com.gangku.be.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class SignUpResponseDto {

    private final String id;
    private final String email;
    private final String nickname;
    private final String profileObjectKey;
    private final Integer age;
    private final String gender;
    private final Integer enrollNumber;
    private final List<String> preferredCategories;
    private final LocalDateTime createdAt;

    public static SignUpResponseDto from(User user) {
        List<String> preferredCategoryNames = user.getPreferredCategories().stream()
                .map(preferredCategory -> preferredCategory.getCategory().getName())
                .toList();

        return new SignUpResponseDto(
                "usr_" + user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileObjectKey(),
                user.getAge(),
                user.getGender(),
                user.getEnrollNumber(),
                preferredCategoryNames,
                user.getCreatedAt()
        );
    }
}