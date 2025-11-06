package com.gangku.be.dto.gathering.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GatheringUpdateResponseDto {
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
    private String updatedAt;
}