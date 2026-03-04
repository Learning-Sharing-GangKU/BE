package com.gangku.be.domain;

import com.gangku.be.constant.participation.ParticipationRole;
import com.gangku.be.constant.participation.ParticipationStatus;
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
                    columnNames = {"gathering_id", "user_id"})
        })
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
    private ParticipationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipationRole role;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    public void prePersist() {
        this.joinedAt = LocalDateTime.now();
    }

    public static Participation create(User user, Gathering gathering, ParticipationRole role) {
        Participation participation =
                Participation.builder()
                        .user(user)
                        .gathering(gathering)
                        .status(ParticipationStatus.APPROVED)
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
