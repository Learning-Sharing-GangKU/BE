package com.gangku.be.controller;

import com.gangku.be.dto.gathering.response.GatheringListResponseDto;
import com.gangku.be.service.GatheringService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final GatheringService gatheringService;

    @GetMapping()
    public ResponseEntity<Map<String, GatheringListResponseDto>> getHomeGatherings() {
        Map<String, GatheringListResponseDto> result = new HashMap<>();

        result.put("recommended", gatheringService.getGatheringList(null, 1, 3, "latest")); // Recommend 로직 이후에 추가 임시
        result.put("latest", gatheringService.getGatheringList(null, 1, 3, "latest"));
        result.put("popular", gatheringService.getGatheringList(null, 1, 3, "popular"));

        return ResponseEntity.ok(result);
    }
}
