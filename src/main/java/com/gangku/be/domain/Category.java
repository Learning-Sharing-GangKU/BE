package com.gangku.be.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@EntityListeners(AuditingEntityListener.class) // 생성일/수정일 자동 관리 활성화
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 기본 키(PK), DB에서 자동 증가 (AUTO_INCREMENT) 방식으로 생성됨
    private Long id;

    @Column(unique = true, nullable = false)
    // 카테고리 이름은 중복 불가 & 반드시 입력되어야 함
    private String name;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    // 레코드 생성 시 자동으로 현재 시간 저장 (한 번만)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    // 레코드 수정 시 자동으로 현재 시간 갱신
    private LocalDateTime updatedAt;

}
