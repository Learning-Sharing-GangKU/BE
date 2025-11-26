package com.gangku.be.domain;

import com.gangku.be.dto.user.SignUpRequestDto;
import com.gangku.be.dto.user.SignUpRequestDto.ProfileImage;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name = "users")  // 예약어 피하기 위해 users 사용
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

    @Column(nullable = false, length = 2000)
    private String profileObjectKey;

    private Boolean emailVerified;

    private Boolean reviewsPublic;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "refresh_expiry")
    private LocalDateTime refreshExpiry;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PreferredCategory> preferredCategories = new ArrayList<>();

    //자동 시간 설정을 위한 콜백 메서드
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
        preferredCategory.setUser(this);
    }

    public static User create(
            String email,
            String encodedPassword,
            String nickname,
            Integer age,
            String gender,
            Integer enrollNumber,
            ProfileImage profileImage
    ) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .nickname(nickname)
                .age(age)
                .gender(gender)
                .enrollNumber(enrollNumber)
                .profileObjectKey(
                        profileImage.getBucket() +
                                "/" +
                                profileImage.getKey()
                )
                .emailVerified(false)
                .reviewsPublic(true)
                .build();
    }
}