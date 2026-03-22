package com.gangku.be.dto.ai.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularityRefreshResponseDto {

    @JsonProperty("total_logs")
    private int totalLogs;

    @JsonProperty("n_clusters")
    private int nClusters;

    @JsonProperty("top_n")
    private int topN;

    @JsonProperty("cluster_popularity")
    private Map<Integer, List<Integer>> clusterPopularity;
}
