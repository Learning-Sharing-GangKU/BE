package com.gangku.be.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Entity
@Table(
        name = "participations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_participations_gathering_user",
                        columnNames = {"gathering_id", "user_id"}
                )
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gathering_id", nullable = false)
    private Gathering gathering;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    public enum Status {
        APPROVED,
        PENDING,
        CANCELED
    }

    public enum Role {
        HOST,
        GUEST
    }

    @PrePersist
    public void prePersist() {
        this.joinedAt = LocalDateTime.now();
    }

    public static Participation create(User user, Gathering gathering, Role role) {
        Participation participation =
                Participation.builder()
                        .user(user)
                        .gathering(gathering)
                        .status(Status.APPROVED)
                        .role(role)
                        .build();

        participation.link(user, gathering);

        return participation;
    }

    private void link(User user, Gathering gathering) {
        user.getParticipations().add(this);
        gathering.getParticipations().add(this);
    }

    public void unlink() {
        this.user.getParticipations().remove(this);
        this.gathering.getParticipations().remove(this);
    }

    public void withdraw() {
        this.gathering.decreaseParticipantCount();

        unlink();
    }
}
