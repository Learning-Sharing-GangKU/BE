package com.gangku.be.model.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record ClusteringRefreshResponse(
        @JsonProperty("n_users")
        int nUsers,

        @JsonProperty("n_clusters")
        int nClusters,

        @JsonProperty("inertia")
        float inertia,

        @JsonProperty("cluster_sizes")
        Map<Integer, Integer> clusterSizes
) {
}