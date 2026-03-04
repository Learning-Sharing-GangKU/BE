package com.gangku.be.service;

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
                .recommended(gatheringService.getRecommendedGatherings(userId, page, size))
                .latest(gatheringService.getGatheringList(null, page, size, "latest"))
                .popular(gatheringService.getGatheringList(null, page, size, "popular"))
                .build();
    }
}
