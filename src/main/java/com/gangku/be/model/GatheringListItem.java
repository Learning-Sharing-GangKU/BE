package com.gangku.be.model;

import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.User;

public record GatheringListItem(
        String id,
        String gatheringImage,
        String category,
        String title,
        String hostName,
        int participantCount,
        int capacity
) {
    public static GatheringListItem from(Gathering gathering) {
        User host = gathering.getHost();

        return new GatheringListItem(
                String.valueOf(gathering.getId()),
                gathering.getGatheringImageObject(),
                gathering.getCategory().getName(),
                gathering.getTitle(),
                host.getNickname(),
                gathering.getParticipantCount(),
                gathering.getCapacity()
        );
    }
}
