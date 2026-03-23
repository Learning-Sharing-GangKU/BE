package com.gangku.be.service;

import com.gangku.be.domain.User;
import com.gangku.be.domain.UserActionCollection;
import com.gangku.be.dto.ai.request.ClusteringRefreshRequestDto;
import com.gangku.be.dto.ai.request.ClusteringRefreshRequestDto.ClusteringUserData;
import com.gangku.be.dto.ai.request.PopularityRefreshRequestDto;
import com.gangku.be.model.ai.ClusteringRefreshResponse;
import com.gangku.be.external.ai.AiApiClient;
import com.gangku.be.model.ai.PopularityRefreshResponse;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.UserActionCollectionRepository;
import com.gangku.be.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClusteringService {

    private final UserRepository userRepository;
    private final ParticipationRepository participationRepository;
    private final UserActionCollectionRepository actionCollectionRepository;
    private final AiApiClient aiApiClient;

    @Transactional(readOnly = true)
    public void refreshClustering() {
        // 1) 전체 유저 조회
        List<User> users = userRepository.findAll();

        // 2-0) DB 에서 따온 userCount 한 번에 관리
        Map<Long, Integer> joinCountMap =
                participationRepository.countApprovedParticipationGroupByUserId().stream()
                        .collect(
                                Collectors.toMap(
                                        row -> (Long) row[0], row -> ((Long) row[1]).intValue()));

        // 2-1) DTO 조립
        List<ClusteringUserData> clusteringUserDataList =
                users.stream()
                        .map(
                                user ->
                                        ClusteringUserData.builder()
                                                .userId(user.getId().intValue())
                                                .preferredCategories(
                                                        user.getPreferredCategories().stream()
                                                                .map(
                                                                        pc ->
                                                                                pc.getCategory()
                                                                                        .getName())
                                                                .toList())
                                                .age(user.getAge())
                                                .enrollNumber(user.getEnrollNumber())
                                                .userJoinCount(
                                                        joinCountMap.getOrDefault(user.getId(), 0))
                                                .build())
                        .toList();

        ClusteringRefreshRequestDto request =
                ClusteringRefreshRequestDto.builder().users(clusteringUserDataList).build();

        // 3) AI 호출
        ClusteringRefreshResponse response = aiApiClient.refreshClustering(request);

        log.info(
                "클러스터링 완료 - 유저수: {}, 클러스터수: {}, inertia: {}, 클러스터별 유저수: {}",
                response.nUsers(),
                response.nClusters(),
                response.inertia(),
                response.clusterSizes());
    }

    public void refreshPopularity() {
        // 1) 전체 로그리스트 조회
        List<UserActionCollection> logList =
                actionCollectionRepository.findAllWithUserAndGathering();

        // 2) DTO 조립
        List<PopularityRefreshRequestDto.UserActionLog> clusteringPopularityDataList =
                logList.stream()
                        .map(
                                ua ->
                                        PopularityRefreshRequestDto.UserActionLog.builder()
                                                .userId(ua.getUser().getId().intValue())
                                                .gatheringId(ua.getGathering().getId().intValue())
                                                .status(ua.getStatus().name())
                                                .build())
                        .toList();

        PopularityRefreshRequestDto request =
                PopularityRefreshRequestDto.builder().logList(clusteringPopularityDataList).build();

        // 3) AI 호출 (response 로 뭘 하는게 아니므로 단순 호출만)
        PopularityRefreshResponse response = aiApiClient.refreshPopularity(request);

        log.info(
                "클러스터링 완료 - 토탈 로그 수: {}, 클러스터수: {}, top N 수: {}, 클러스터별 Popularity: {}",
                response.totalLogs(),
                response.nClusters(),
                response.topN(),
                response.clusterPopularity());
    }
}
