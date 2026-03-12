package com.gangku.be.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 20)
    private String nickname;

    private Integer age;

    @Column(length = 50)
    private String gender;

    private Integer enrollNumber;

    @Column(name = "profile_image_object_key", length = 255)
    private String profileImageObjectKey;

    @Column(name = "review_public")
    private Boolean reviewPublic;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "refresh_expiry")
    private LocalDateTime refreshExpiry;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PreferredCategory> preferredCategories = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Participation> participations = new ArrayList<>();

    // 자동 시간 설정을 위한 콜백 메서드
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateRefreshToken(String refreshToken, LocalDateTime refreshExpiry) {
        this.refreshToken = refreshToken;
        this.refreshExpiry = refreshExpiry;
    }

    public void clearRefreshToken() {
        this.refreshToken = null;
        this.refreshExpiry = null;
    }

    public void addPreferredCategory(PreferredCategory preferredCategory) {
        this.preferredCategories.add(preferredCategory);
        preferredCategory.assignUser(this);
    }

    public void updateProfile(
            String profileImageObjectKey,
            String nickname,
            Integer age,
            String gender,
            Integer enrollNumber) {
        if (profileImageObjectKey != null) this.profileImageObjectKey = profileImageObjectKey;
        if (nickname != null) this.nickname = nickname;
        if (age != null) this.age = age;
        if (gender != null) this.gender = gender;
        if (enrollNumber != null) this.enrollNumber = enrollNumber;
    }

    public void changeReviewPublic(Boolean reviewPublic) {
        this.reviewPublic = reviewPublic;
    }

    public static User create(
            String email,
            String encodedPassword,
            String nickname,
            Integer age,
            String gender,
            Integer enrollNumber,
            String profileImageObjectKey) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .nickname(nickname)
                .age(age)
                .gender(gender)
                .enrollNumber(enrollNumber)
                .profileImageObjectKey(profileImageObjectKey)
                .reviewPublic(true)
                .build();
    }
}
