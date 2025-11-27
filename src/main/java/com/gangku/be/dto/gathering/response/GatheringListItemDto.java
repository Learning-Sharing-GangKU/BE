package com.gangku.be.dto.gathering.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatheringListItemDto {
    private String id;
    private String imageUrl;
    private String category;
    private String title;
    private String hostName;
    private int participantCount;
    private int capacity;
}