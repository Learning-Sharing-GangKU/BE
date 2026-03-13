package com.gangku.be.dto.ai.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.User;
import com.gangku.be.model.ai.RecommendationGatheringItem;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RecommendationRequestDto {

    private Long userId;
    private List<String> preferredCategories;
    private Integer age;
    private Integer enrollNumber;

    @JsonProperty("gatherings")
    private List<RecommendationGatheringItem> gatheringItems;

    public static RecommendationRequestDto from(
            User user, List<String> preferredCategories, List<Gathering> gatherings) {
        List<RecommendationGatheringItem> items =
                gatherings.stream().map(RecommendationGatheringItem::from).toList();

        return RecommendationRequestDto.builder()
                .userId(user.getId())
                .preferredCategories(preferredCategories)
                .age(user.getAge())
                .enrollNumber(user.getEnrollNumber())
                .gatheringItems(items)
                .build();
    }
}
