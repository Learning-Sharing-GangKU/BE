// src/main/java/com/gangku/be/controller/GatheringController.java

package com.gangku.be.controller;

import com.gangku.be.domain.User;
import com.gangku.be.dto.gathering.*;
import com.gangku.be.dto.gathering.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.GatheringCreateResponseDto;
import com.gangku.be.service.GatheringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/gatherings")
@RequiredArgsConstructor
public class GatheringController {

    private final GatheringService gatheringService;

    @PostMapping
    public ResponseEntity<GatheringCreateResponseDto> createGathering(
            @RequestBody GatheringCreateRequestDto requestDto,
            @AuthenticationPrincipal User user  // JWT에서 userId 추출됨
    ) {

        // 모임 생성
        GatheringCreateResponseDto responseDto = gatheringService.createGathering(requestDto, user);

        // 응답 구성
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/api/v1/gatherings/" + responseDto.getId()));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .headers(headers)
                .body(responseDto);
    }

    @PatchMapping("/{gatheringId}")
    public ResponseEntity<GatheringUpdateResponseDto> updateGathering(
            @PathVariable Long gatheringId,
            @AuthenticationPrincipal Long userId,
            @RequestBody GatheringUpdateRequestDto requestDto) {
        GatheringUpdateResponseDto updated = gatheringService.updateGathering(gatheringId, userId, requestDto);
        return ResponseEntity.ok(updated);
    }

    // 모임 삭제
    @DeleteMapping("/{gatheringId}")
    public ResponseEntity<Void> deleteGathering(
            @PathVariable("gatheringId") Long gatheringId,
            @AuthenticationPrincipal Long userId
    ) {
        gatheringService.deleteGathering(gatheringId, userId);
        return ResponseEntity.noContent().build();
    }

}