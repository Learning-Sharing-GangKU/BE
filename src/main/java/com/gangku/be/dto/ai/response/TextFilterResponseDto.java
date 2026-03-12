package com.gangku.be.dto.ai.response;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TextFilterResponseDto {

    private boolean allowed;
    private Double score;
    private Matches matches;

    @Getter
    @NoArgsConstructor
    public static class Matches {
        private List<BlacklistMatch> blacklistMatchList;
        private String route;
        private Double threshold;
    }

    @Getter
    @NoArgsConstructor
    public static class BlacklistMatch {
        private String category;
        private String match;
        private Integer start;
        private Integer end;
    }
}
