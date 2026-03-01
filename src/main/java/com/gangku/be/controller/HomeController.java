package com.gangku.be.controller;

import com.gangku.be.dto.home.response.HomeResponseDto;
import com.gangku.be.service.GatheringService;
import com.gangku.be.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final GatheringService gatheringService;
    private final HomeService homeService;

    @GetMapping()
    public ResponseEntity<HomeResponseDto> getHomeGatherings(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(homeService.getHome(userId, 1, 3));
    }
}
