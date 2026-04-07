package com.gangku.be.model.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record PopularityRefreshResponse(
        @JsonProperty("total_logs") int totalLogs,
        @JsonProperty("n_clusters") int nClusters,
        @JsonProperty("top_n") int topN,
        @JsonProperty("cluster_popularity") Map<Integer, List<Integer>> clusterPopularity) {}
