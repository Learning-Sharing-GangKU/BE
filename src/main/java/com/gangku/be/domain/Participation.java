package com.gangku.be.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "participations")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Gathering gathering;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime JoinedAt;

    public enum Status {
        APPROVED,     // 정상 참여
        PENDING,
        CANCELED    // 참여 취소
    }

    @PrePersist
    public void prePersist() {
        this.JoinedAt = LocalDateTime.now();
    }


    // Getter/Setter 생략
}