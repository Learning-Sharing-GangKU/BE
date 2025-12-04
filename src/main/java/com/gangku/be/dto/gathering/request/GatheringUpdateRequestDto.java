package com.gangku.be.dto.gathering.request;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatheringUpdateRequestDto {

    private String title;
    private String gatheringImageObjectKey;
    private String category;
    private Integer capacity;
    private LocalDateTime date;
    private String location;
    private String openChatUrl;
    private String description;
}