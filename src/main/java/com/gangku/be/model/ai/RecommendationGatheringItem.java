package com.gangku.be.model.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.User;
import java.time.LocalDateTime;

public record RecommendationGatheringItem(
        Long gatheringId,
        String category,
        Integer hostAge,
        Integer capacity,
        Integer participantCount,
        @JsonFormat(
                        shape = JsonFormat.Shape.STRING,
                        pattern = "yyyy-MM-dd'T'HH:mm:ss",
                        timezone = "Asia/Seoul")
                LocalDateTime createdAt) {

    public static RecommendationGatheringItem from(Gathering gathering) {
        User host = gathering.getHost();

        return new RecommendationGatheringItem(
                gathering.getId(),
                gathering.getCategory().getName(),
                host.getAge(),
                gathering.getCapacity(),
                gathering.getParticipantCount(),
                gathering.getCreatedAt());
    }
}
