package com.gangku.be.model.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.User;

import java.time.LocalDateTime;

public record AiRecommendGatheringItem(
        Long gatheringId,
        String category,
        @JsonProperty("host_age") Integer hostAge,
        Integer capacity,
        Integer participantCount,
        LocalDateTime createdAt
) {

    public static AiRecommendGatheringItem from(Gathering gathering) {
        User host = gathering.getHost();

        return new AiRecommendGatheringItem(
                gathering.getId(),
                gathering.getCategory().getName(),
                host.getAge(),
                gathering.getCapacity(),
                gathering.getParticipantCount(),
                gathering.getCreatedAt()
        );
    }
}
