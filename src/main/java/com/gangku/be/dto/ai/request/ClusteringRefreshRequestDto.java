package com.gangku.be.dto.ai.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClusteringRefreshRequestDto {
    private List<ClusteringUserData> users;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClusteringUserData {
        private Integer userId;
        private List<String> preferredCategories;
        private Integer age;
        private Integer enrollNumber;
        private Integer userJoinCount;
    }
}
