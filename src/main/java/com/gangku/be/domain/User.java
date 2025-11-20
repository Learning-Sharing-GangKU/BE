package com.gangku.be.domain;

import com.gangku.be.dto.user.SignUpRequestDto;
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
            SignUpRequestDto signUpRequestDto,
            String encodedPassword
    ) {
        return User.builder()
                .email(signUpRequestDto.getEmail())
                .password(encodedPassword)
                .nickname(signUpRequestDto.getNickname())
                .age(signUpRequestDto.getAge())
                .gender(signUpRequestDto.getGender())
                .enrollNumber(signUpRequestDto.getEnrollNumber())
                .profileObjectKey(
                        signUpRequestDto.getProfileImage().getBucket()
                                + "/"
                                +  signUpRequestDto.getProfileImage().getKey()
                )
                .emailVerified(false)
                .reviewsPublic(true)
                .createdAt(null)     // @PrePersist로 자동 설정됨
                .updatedAt(null)     // @PrePersist/@PreUpdate로 자동 설정됨
                .build();
    }
}