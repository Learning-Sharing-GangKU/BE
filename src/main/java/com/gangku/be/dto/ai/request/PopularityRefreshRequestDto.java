package com.gangku.be.dto.ai.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
