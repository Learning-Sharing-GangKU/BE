package com.gangku.be.service;

import com.gangku.be.constant.cache.CacheKeys;
import com.gangku.be.constant.gathering.GatheringSort;
import com.gangku.be.dto.gathering.response.GatheringListResponseDto;
import com.gangku.be.dto.home.response.HomeResponseDto;
import com.gangku.be.util.cache.HomeCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final GatheringService gatheringService;
    private final HomeCache homeCache;

    public HomeResponseDto getHome(Long userId, int page, int size) {
        GatheringListResponseDto recommended =
                homeCache.getOrFetch(
                        CacheKeys.homeRecommend(userId, page, size),
                        HomeCache.TTL_RECOMMEND,
                        () ->
                                gatheringService.getGatheringList(
                                        userId,
                                        null,
                                        page,
                                        size,
                                        GatheringSort.RECOMMEND.getSort()));

        GatheringListResponseDto latest =
                homeCache.getOrFetch(
                        CacheKeys.homeLatest(page, size),
                        HomeCache.TTL_LATEST,
                        () ->
                                gatheringService.getGatheringList(
                                        userId,
                                        null,
                                        page,
                                        size,
                                        GatheringSort.LATEST.getSort()));

        GatheringListResponseDto popular =
                homeCache.getOrFetch(
                        CacheKeys.homePopular(page, size),
                        HomeCache.TTL_POPULAR,
                        () ->
                                gatheringService.getGatheringList(
                                        userId,
                                        null,
                                        page,
                                        size,
                                        GatheringSort.POPULAR.getSort()));

        return HomeResponseDto.builder()
                .recommended(recommended)
                .latest(latest)
                .popular(popular)
                .build();
    }
}
