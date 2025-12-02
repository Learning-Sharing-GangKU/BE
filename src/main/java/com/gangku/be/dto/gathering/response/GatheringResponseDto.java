package com.gangku.be.dto.gathering.response;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    private String imageUrl;
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


    public static GatheringResponseDto from(Gathering gathering) {

        return GatheringResponseDto.builder()
                .id("gath_" + gathering.getId())
                .title(gathering.getTitle())
                .category(gathering.getCategory().getName())
                .capacity(gathering.getCapacity())
                .date(gathering.getDate())
                .location(gathering.getLocation())
                .openChatUrl(gathering.getOpenChatUrl())
                .description(gathering.getDescription())
                .hostId("usr_" + gathering.getHost().getId())
                .createdAt(gathering.getCreatedAt())
                .build();
    }
}