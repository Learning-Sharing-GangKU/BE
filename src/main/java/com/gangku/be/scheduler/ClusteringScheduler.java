package com.gangku.be.scheduler;

import com.gangku.be.service.ClusteringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClusteringScheduler {
    private final ClusteringService clusteringService;

    @Scheduled(cron = "0 0 5 * * *") // 매일 새벽 5시
    public void refreshClustering() {
        clusteringService.refreshClustering();
        clusteringService.refreshPopularity();
    }

    @Scheduled(cron = "0 0 */2 * * *") // 2시간마다
    public void refreshPopularity() {
        clusteringService.refreshPopularity();
    }
}
