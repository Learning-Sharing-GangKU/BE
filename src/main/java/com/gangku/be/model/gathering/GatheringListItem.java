package com.gangku.be.model.gathering;

import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.User;
import com.gangku.be.model.common.PrefixedId;

public record GatheringListItem(
        String id,
        String gatheringImageUrl,
        String category,
        String title,
        String hostName,
        int participantCount,
        int capacity) {
    public static GatheringListItem from(Gathering gathering, String gatheringImageUrl) {
        User host = gathering.getHost();

        String publicGatheringId =
                PrefixedId.of(ResourceType.GATHERING, gathering.getId()).toExternal();

        return new GatheringListItem(
                publicGatheringId,
                gatheringImageUrl,
                gathering.getCategory().getName(),
                gathering.getTitle(),
                host.getNickname(),
                gathering.getParticipantCount(),
                gathering.getCapacity());
    }
}
