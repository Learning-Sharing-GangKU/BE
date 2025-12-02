package com.gangku.be.dto.gathering.response;

import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.model.HostSummary;
import com.gangku.be.model.ParticipantsPreview;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@Builder
@AllArgsConstructor
public class GatheringDetailResponseDto {

    private Long id;
    private String title;
    private String gatheringImage;
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
            Page<Participation> participationPage,
            int page,
            int size,
            String sortedBy
    ) {

        HostSummary host = HostSummary.from(gathering.getHost());

        ParticipantsPreview participantsPreview =
                ParticipantsPreview.from(
                        participationPage,
                        page,
                        size,
                        sortedBy
                );

        return GatheringDetailResponseDto.builder()
                .id(gathering.getId())
                .title(gathering.getTitle())
                .gatheringImage(gathering.getGatheringImageObject())
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
