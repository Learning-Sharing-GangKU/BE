package com.gangku.be.dto.gathering;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class GatheringUpdateRequestDto {

    private String title;
    private String imageUrl;
    private String category;
    private Integer capacity;
//    private String date; // ISO-8601 (String → LocalDateTime으로 변환)
    private LocalDateTime date;
    private String location;
    private String openChatUrl;
    private String description;
}