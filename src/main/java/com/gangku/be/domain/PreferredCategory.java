package com.gangku.be.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 선호 카테고리를 나타내는 엔티티
 * 사용자(User)와 카테고리(Category)의 다대다(N:M) 관계를 매핑
 */
@Entity
@Table(name = "preferred_categories", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "category_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class PreferredCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 기본 키 자동 생성
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 다대일 관계 (여러 선호카테고리 → 한 사용자)
    @JoinColumn(name = "user_id", nullable = false) // 외래키 매핑
    private User user;

    @ManyToOne(fetch = FetchType.LAZY) // 다대일 관계 (여러 선호카테고리 → 한 카테고리)
    @JoinColumn(name = "category_id", nullable = false) // 외래키 매핑
    private Category category;
}