package com.gangku.be.dto.gathering.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.model.common.PrefixedId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import com.gangku.be.domain.Gathering;

import java.time.LocalDateTime;

@Builder
@Getter
@AllArgsConstructor
public class GatheringResponseDto {

    private String id;
    private String title;
    private String gatheringImageUrl;
    private String category;
    private int capacity;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime date;

    private String location;
    private String openChatUrl;
    private String description;
    private String hostId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime createdAt;


    public static GatheringResponseDto from(Gathering gathering, String gatheringImageUrl) {
        String publicGatheringId = PrefixedId.of(ResourceType.GATHERING, gathering.getId()).toExternal();
        String publicUserId = PrefixedId.of(ResourceType.USER, gathering.getHost().getId()).toExternal();

        return GatheringResponseDto.builder()
                .id(publicGatheringId)
                .title(gathering.getTitle())
                .gatheringImageUrl(gatheringImageUrl)
                .category(gathering.getCategory().getName())
                .capacity(gathering.getCapacity())
                .date(gathering.getDate())
                .location(gathering.getLocation())
                .openChatUrl(gathering.getOpenChatUrl())
                .description(gathering.getDescription())
                .hostId(publicUserId)
                .createdAt(gathering.getCreatedAt())
                .build();
    }
}