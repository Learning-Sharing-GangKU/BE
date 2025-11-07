package com.gangku.be.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gatherings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gathering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 호스트: User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    // 🔗 카테고리
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "participant_count", nullable = false)
    @Builder.Default
    private Integer participantCount = 0;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(length = 255)
    private String location;

    @Column(name = "openchat_url", nullable = false, length = 50, unique = true)
    private String openChatUrl;

    // 상태 ENUM
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    public enum Status {
        RECRUITING, FULL, FINISHED
    }

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // cascade 로 모임 삭제 시 참여자도 삭제 , orphanRemoval=true 로 연관 끊기면 DB에서 제거
    @OneToMany(mappedBy = "gathering", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Participation> participations = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
//        this.participantCount = 0;
        this.status = Status.RECRUITING;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 양방향 연관관계 편의 메서드
    public void addParticipation(Participation participation) {
        participations.add(participation);
        participation.setGathering(this);
    }

    public void removeParticipation(Participation participation) {
        participations.remove(participation);
        participation.setGathering(null);
    }
}