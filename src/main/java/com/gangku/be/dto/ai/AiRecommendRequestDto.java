package com.gangku.be.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.User;
import com.gangku.be.model.ai.AiRecommendGatheringItem;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AiRecommendRequestDto {

    private Long userId;
    private List<String> preferredCategories;
    private Integer age;

    @JsonProperty("gatherings")
    private List<AiRecommendGatheringItem> gatheringItems;

    public static AiRecommendRequestDto from(
            User user,
            List<String> preferredCategories,
            List<Gathering> gatherings
    ) {
        List<AiRecommendGatheringItem> items = gatherings.stream()
                .map(AiRecommendGatheringItem::from)
                .toList();

        return AiRecommendRequestDto.builder()
                .userId(user.getId())
                .preferredCategories(preferredCategories)
                .age(user.getAge())
                .gatheringItems(items)
                .build();
    }
}
