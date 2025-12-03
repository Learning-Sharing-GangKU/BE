package com.gangku.be.dto.participation;

import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import com.gangku.be.model.PrefixedId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
@AllArgsConstructor
public class ParticipationResponseDto {
    private String participantId;
    private String gatheringId;
    private String userId;
    private String role;
    private int participantCount;
    private int capacity;
    private LocalDateTime joinedAt;

    public static ParticipationResponseDto from(Participation participation, Gathering gathering, User user) {
        String publicParticipationId = PrefixedId.of(ResourceType.PARTICIPATION, participation.getId()).toExternal();
        String publicGatheringId = PrefixedId.of(ResourceType.GATHERING, gathering.getId()).toExternal();
        String publicUserId = PrefixedId.of(ResourceType.USER, gathering.getHost().getId()).toExternal();

        return ParticipationResponseDto.builder()
                .participantId(publicParticipationId)
                .gatheringId(publicGatheringId)
                .userId(publicUserId)
                .role(participation.getRole().name())
                .participantCount(gathering.getParticipantCount())
                .capacity(gathering.getCapacity())
                .joinedAt(participation.getJoinedAt())
                .build();
    }
}