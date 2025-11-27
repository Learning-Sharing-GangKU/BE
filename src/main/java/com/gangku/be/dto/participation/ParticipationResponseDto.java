package com.gangku.be.dto.participation;

import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
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

        return ParticipationResponseDto.builder()
                .gatheringId("gath_" + gathering.getId())
                .participantId("part_" + participation.getId())
                .userId("usr_" + user.getId())
                .role(participation.getRole().name())
                .participantCount(gathering.getParticipantCount())
                .capacity(gathering.getCapacity())
                .joinedAt(participation.getJoinedAt())
                .build();
    }
}