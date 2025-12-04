package com.gangku.be.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
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

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    private Gathering gathering;

    @Enumerated(EnumType.STRING)
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
        return Participation.builder()
                .user(user)
                .gathering(gathering)
                .status(Status.APPROVED)
                .role(role)
                .build();
    }
}