package com.gangku.be.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

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

    @Column(name = "gathering_image_object_key", length = 255)
    private String gatheringImageObjectKey;

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
        RECRUITING,
        FULL,
        FINISHED
    }

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "gathering", cascade = CascadeType.REMOVE, orphanRemoval = true)
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

    public void increaseParticipantCount() {
        this.participantCount++;

        if (this.participantCount >= this.capacity) {
            this.status = Status.FULL;
        }
    }

    public void decreaseParticipantCount() {
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
            String gatheringImageObjectKey,
            Integer capacity,
            LocalDateTime date,
            String location,
            String openChatUrl) {
        return Gathering.builder()
                .host(host)
                .category(category)
                .title(title)
                .description(description)
                .gatheringImageObjectKey(gatheringImageObjectKey)
                .capacity(capacity)
                .participantCount(1)
                .date(date)
                .location(location)
                .openChatUrl(openChatUrl)
                .status(Status.RECRUITING)
                .build();
    }
}
