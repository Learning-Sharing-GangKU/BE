package com.gangku.be.dto.ai.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularityRefreshRequestDto {

    private List<UserActionLog> logList;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActionLog {
        private Integer userId;
        private Integer gatheringId;
        private String status;
    }
}
