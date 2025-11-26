package com.gangku.be.dto.participation;

import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ParticipationResponseDto {
    private String gatheringId;
    private String participantId;
    private String userId;
    private String role;
    private int participantCount;
    private int capacity;
    private LocalDateTime joinedAt;

    public static ParticipationResponseDto from(Participation participation, Gathering gathering, User user) {
        return new ParticipationResponseDto(
                "gath_" + gathering.getId(),
                "part_" + participation.getId(),
                "usr_" + user.getId(),
                "guest",
                gathering.getParticipantCount(),
                gathering.getCapacity(),
                participation.getJoinedAt()
        );
    }
}