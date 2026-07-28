package com.gangku.be.model.gathering;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.domain.Gathering;
import com.gangku.be.model.common.PrefixedId;
import java.time.LocalDateTime;

public record GatheringListItem(
        String id,
        String gatheringImageUrl,
        String category,
        String title,
        String description,
        String location,
        @JsonFormat(
                        shape = JsonFormat.Shape.STRING,
                        pattern = "yyyy-MM-dd'T'HH:mm:ss",
                        timezone = "Asia/Seoul")
                LocalDateTime date) {
    public static GatheringListItem from(Gathering gathering, String gatheringImageUrl) {

        String publicGatheringId =
                PrefixedId.of(ResourceType.GATHERING, gathering.getId()).toExternal();

        return new GatheringListItem(
                publicGatheringId,
                gatheringImageUrl,
                gathering.getCategory().getName(),
                gathering.getTitle(),
                gathering.getDescription(),
                gathering.getLocation(),
                gathering.getDate());
    }
}
