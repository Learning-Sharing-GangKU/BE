package com.gangku.be.dto.participation;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ParticipationResponseDto {
    private String gatheringId;
    private String participantId;
    private String userId;
    private String role; // 예: "guest"
    private int participantCount;
    private int capacity;
    private LocalDateTime joinedAt;
}