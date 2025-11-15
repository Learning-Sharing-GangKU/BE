// src/main/java/com/gangku/BE/domain/User.java

package com.gangku.be.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
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
    private String photoUrl;

    private Boolean emailVerified;

    private Boolean reviewsPublic;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "refresh_expiry")
    private LocalDateTime refreshExpiry;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ✅ @ElementCollection 제거하고 PreferredCategory로 대체
    @Builder.Default // 값을 따로 주지 않을경우 new ArrayList가 기본값으로 들어감
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

    public String getProfileImageUrl() {
        return this.photoUrl;
    }

    public void updateRefreshToken(String refreshToken, LocalDateTime refreshExpiry) {
        this.refreshToken = refreshToken;
        this.refreshExpiry = refreshExpiry;
    }

    public void clearRefreshToken() {
        this.refreshToken = null;
        this.refreshExpiry = null;
    }
}