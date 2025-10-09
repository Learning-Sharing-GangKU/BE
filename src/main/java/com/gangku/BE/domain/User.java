// src/main/java/com/gangku/BE/domain/User.java

package com.gangku.BE.domain;

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
    @Column(name = "user_id", length = 36)
    private String userId;

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

    // 카테고리 선호 (단방향 ManyToMany → JoinTable 사용 가능)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "preferred_categories", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "category")
    private List<String> preferredCategories = new ArrayList<>(); //지금은 List<String>으로 처리하지만 추후 ENUM + 별도 테이블로 확장해야함.

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
}