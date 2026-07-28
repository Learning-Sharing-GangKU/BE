package com.gangku.be.service;

import com.gangku.be.constant.cache.CacheKeys;
import com.gangku.be.constant.gathering.GatheringSort;
import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.dto.gathering.response.GatheringListResponseDto;
import com.gangku.be.dto.home.response.HomeResponseDto;
import com.gangku.be.model.common.PrefixedId;
import com.gangku.be.model.gathering.CacheableGatheringList;
import com.gangku.be.model.gathering.GatheringListItem;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.projection.ParticipantCountProjection;
import com.gangku.be.util.cache.HomeCache;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeService {

    private final GatheringService gatheringService;
    private final GatheringRepository gatheringRepository;
    private final HomeCache homeCache;

    public HomeResponseDto getHome(Long userId, int page, int size) {
        CacheableGatheringList recommended =
                homeCache.getOrFetch(
                        CacheKeys.homeRecommend(userId, page, size),
                        HomeCache.TTL_RECOMMEND,
                        () ->
                                gatheringService.getCacheableGatheringList(
                                        userId,
                                        null,
                                        page,
                                        size,
                                        GatheringSort.RECOMMEND.getSort()));

        CacheableGatheringList latest =
                homeCache.getOrFetch(
                        CacheKeys.homeLatest(page, size),
                        HomeCache.TTL_LATEST,
                        () ->
                                gatheringService.getCacheableGatheringList(
                                        userId, null, page, size, GatheringSort.LATEST.getSort()));

        CacheableGatheringList popular =
                homeCache.getOrFetch(
                        CacheKeys.homePopular(page, size),
                        HomeCache.TTL_POPULAR,
                        () ->
                                gatheringService.getCacheableGatheringList(
                                        userId, null, page, size, GatheringSort.POPULAR.getSort()));

        Map<Long, Integer> participantCounts = fetchParticipantCounts(recommended, latest, popular);

        return HomeResponseDto.builder()
                .recommended(mergeParticipantCounts(recommended, participantCounts))
                .latest(mergeParticipantCounts(latest, participantCounts))
                .popular(mergeParticipantCounts(popular, participantCounts))
                .build();
    }

    // 캐시 hit/miss 여부와 무관하게 participantCount는 항상 DB에서 실시간으로 조회해 병합한다.
    // 캐시 히트 시에도 매 요청마다 타는 경로라 DB 장애 노출 면적이 넓다 - 보조 데이터이므로 실패 시 0으로 폴백하고 홈 화면 자체는 계속 내려준다.
    private Map<Long, Integer> fetchParticipantCounts(CacheableGatheringList... lists) {
        Set<Long> gatheringIds =
                Arrays.stream(lists)
                        .flatMap(list -> list.data().stream())
                        .map(item -> PrefixedId.parse(item.id()).require(ResourceType.GATHERING))
                        .collect(Collectors.toSet());

        if (gatheringIds.isEmpty()) {
            return Map.of();
        }

        try {
            return gatheringRepository.findParticipantCountsByIdIn(gatheringIds).stream()
                    .collect(
                            Collectors.toMap(
                                    ParticipantCountProjection::getId,
                                    ParticipantCountProjection::getParticipantCount));
        } catch (DataAccessException e) {
            log.warn("참가자 수 실시간 조회 실패 - 0으로 폴백합니다. gatheringIds={}", gatheringIds, e);
            return Map.of();
        }
    }

    private GatheringListResponseDto mergeParticipantCounts(
            CacheableGatheringList cached, Map<Long, Integer> participantCounts) {
        var items =
                cached.data().stream()
                        .map(
                                item -> {
                                    Long gatheringId =
                                            PrefixedId.parse(item.id())
                                                    .require(ResourceType.GATHERING);
                                    int participantCount =
                                            participantCounts.getOrDefault(gatheringId, 0);
                                    return GatheringListItem.from(item, participantCount);
                                })
                        .toList();

        return GatheringListResponseDto.builder().data(items).meta(cached.meta()).build();
    }
}
