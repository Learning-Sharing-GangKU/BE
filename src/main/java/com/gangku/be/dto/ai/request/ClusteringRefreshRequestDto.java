package com.gangku.be.dto.ai.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
