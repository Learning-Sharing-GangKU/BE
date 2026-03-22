package com.gangku.be.dto.ai.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusteringRefreshResponseDto {
    @JsonProperty("n_users")
    private int nUsers;

    @JsonProperty("n_clusters")
    private int nClusters;

    @JsonProperty("inertia")
    private float inertia;

    @JsonProperty("cluster_sizes")
    private Map<Integer, Integer> clusterSizes;

}
