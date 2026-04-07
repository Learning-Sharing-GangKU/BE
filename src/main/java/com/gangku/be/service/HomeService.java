package com.gangku.be.service;

import com.gangku.be.constant.gathering.GatheringSort;
import com.gangku.be.dto.home.response.HomeResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeService {
    private final GatheringService gatheringService;

    @Transactional(readOnly = true)
    public HomeResponseDto getHome(Long userId, int page, int size) {
        return HomeResponseDto.builder()
                .recommended(
                        gatheringService.getGatheringList(
                                userId, null, page, size, GatheringSort.RECOMMEND.getSort()))
                .latest(
                        gatheringService.getGatheringList(
                                userId, null, page, size, GatheringSort.LATEST.getSort()))
                .popular(
                        gatheringService.getGatheringList(
                                userId, null, page, size, GatheringSort.POPULAR.getSort()))
                .build();
    }
}
