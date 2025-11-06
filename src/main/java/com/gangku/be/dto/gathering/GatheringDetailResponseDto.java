package com.gangku.be.dto.gathering;

import com.gangku.be.domain.Gathering;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class GatheringDetailResponseDto {
    private String id;
    private String title;
    private String imageUrl;
    private String category;
    private int capacity;
    private LocalDateTime date;
    private String location;
    private String openChatUrl;
    private String description;
    private HostDto host;
    private ParticipantsPreviewDto participantsPreview;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GatheringDetailResponseDto from(Gathering g, List<ParticipantPreviewDto> previews, PageMetaDto meta) {
        return GatheringDetailResponseDto.builder()
                .id("gath_" + g.getId())
                .title(g.getTitle())
                .imageUrl(g.getImageUrl())
                .category(g.getCategory().getName())
                .capacity(g.getCapacity())
                .date(g.getDate())
                .location(g.getLocation())
                .openChatUrl(g.getOpenChatUrl())
                .description(g.getDescription())
                .host(HostDto.from(g.getHost()))
                .participantsPreview(new ParticipantsPreviewDto(previews, meta))
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }
}