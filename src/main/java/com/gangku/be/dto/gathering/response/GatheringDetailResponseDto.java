package com.gangku.be.dto.gathering.response;

import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.domain.Gathering;
import com.gangku.be.model.gathering.HostSummary;
import com.gangku.be.model.participation.ParticipantsPreview;
import com.gangku.be.model.common.PrefixedId;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class GatheringDetailResponseDto {

    private String id;
    private String title;
    private String gatheringImageUrl;
    private String category;
    private int capacity;
    private LocalDateTime date;
    private String location;
    private String openChatUrl;
    private String description;
    private HostSummary host;
    private ParticipantsPreview participantsPreview;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GatheringDetailResponseDto from(
            Gathering gathering,
            ParticipantsPreview participantsPreview,
            String gatheringImageUrl
    ) {
        HostSummary host = HostSummary.from(gathering.getHost());

        String publicId = PrefixedId.of(ResourceType.GATHERING, gathering.getId()).toExternal();

        return GatheringDetailResponseDto.builder()
                .id(publicId)
                .title(gathering.getTitle())
                .gatheringImageUrl(gatheringImageUrl)
                .category(gathering.getCategory().getName())
                .capacity(gathering.getCapacity())
                .date(gathering.getDate())
                .location(gathering.getLocation())
                .openChatUrl(gathering.getOpenChatUrl())
                .description(gathering.getDescription())
                .host(host)
                .participantsPreview(participantsPreview)
                .createdAt(gathering.getCreatedAt())
                .updatedAt(gathering.getUpdatedAt())
                .build();
    }
}
