package com.gangku.be.controller;

import com.gangku.be.dto.gathering.*;
import com.gangku.be.dto.gathering.GatheringCreateRequestDto;
import com.gangku.be.service.GatheringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/gatherings")
@RequiredArgsConstructor
public class GatheringController {

    private final GatheringService gatheringService;

    @PostMapping
    public ResponseEntity<GatheringResponseDto> createGathering(
            @RequestBody GatheringCreateRequestDto gatheringCreateRequestDto,
            @AuthenticationPrincipal Long userId
    ) {

        // 모임 생성
        GatheringResponseDto gatheringCreateResponseDto = gatheringService.createGathering(
                gatheringCreateRequestDto,
                userId
        );

        // 응답 구성
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(gatheringCreateResponseDto.getId())
                .toUri();

        return ResponseEntity.created(location).body(gatheringCreateResponseDto);
    }

    @PatchMapping("/{gatheringId}")
    public ResponseEntity<GatheringResponseDto> updateGathering(
            @PathVariable Long gatheringId,
            @AuthenticationPrincipal Long userId,
            @RequestBody GatheringUpdateRequestDto gatheringUpdateRequestDto
    ) {
        GatheringResponseDto updated = gatheringService.updateGathering(gatheringId, userId, gatheringUpdateRequestDto);
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