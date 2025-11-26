package com.gangku.be.dto.gathering;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import com.gangku.be.domain.Gathering;

import java.time.LocalDateTime;

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
        return new GatheringResponseDto(
                "gath_" + gathering.getId(),
                gathering.getTitle(),
                gathering.getImageUrl(),
                gathering.getCategory().getName(),
                gathering.getCapacity(),
                gathering.getDate(),
                gathering.getLocation(),
                gathering.getOpenChatUrl(),
                gathering.getDescription(),
                "usr_" + gathering.getHost().getId(),
                gathering.getCreatedAt()
        );
    }
}