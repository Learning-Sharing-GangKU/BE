package com.gangku.be.domain;

import com.gangku.be.model.ImageObject;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "gathering_image_object", length = 255)
    private String gatheringImageObject;

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
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 양방향 연관관계 편의 메서드
    public void addParticipation(Participation participation) {
        participations.add(participation);
        participation.setGathering(this);

        this.participantCount++;

        if (this.participantCount >= this.capacity) {
            this.status = Status.FULL;
        }
    }

    public void removeParticipation(Participation participation) {
        participations.remove(participation);
        participation.setGathering(null);

        this.participantCount--;

        if (this.participantCount < this.capacity) {
            this.status = Status.RECRUITING;
        }
    }

    public static Gathering create(
            User host,
            Category category,
            String title,
            String description,
            ImageObject gatheringImage,
            Integer capacity,
            LocalDateTime date,
            String location,
            String openChatUrl
    ) {
        return Gathering.builder()
                .host(host)
                .category(category)
                .title(title)
                .description(description)
                .gatheringImageObject(ImageObject.toPathOrNull(gatheringImage))
                .capacity(capacity)
                .participantCount(1)
                .date(date)
                .location(location)
                .openChatUrl(openChatUrl)
                .status(Status.RECRUITING)
                .build();
    }
}