package com.gangku.be.domain;

import com.gangku.be.constant.action.UserAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_action_collection")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActionCollection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gathering_id", nullable = false)
    private Gathering gathering;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserAction status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public static UserActionCollection create(
            User user, Gathering gathering, UserAction status) {
        return UserActionCollection.builder()
                        .user(user)
                        .gathering(gathering)
                        .status(status)
                        .build();
    }
}
