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

    @GetMapping("/{gatheringId}")
    public ResponseEntity<GatheringDetailResponseDto> getGathering(
            @PathVariable Long gatheringId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "joinedAt.asc") String sort
    ) {
        GatheringDetailResponseDto gatheringDetailResponseDto =
                gatheringService.getGathering(gatheringId, page, size, sort);
        return ResponseEntity.ok(gatheringDetailResponseDto);
    }

    // 모임 삭제
    @DeleteMapping("/{gatheringId}")
    public ResponseEntity<Void> deleteGathering(
            @PathVariable Long gatheringId,
            @AuthenticationPrincipal Long userId
    ) {
        gatheringService.deleteGathering(gatheringId, userId);
        return ResponseEntity.noContent().build();
    }

    // AI 모임 정보 생성
    @PostMapping("/intro")
    public ResponseEntity<GatheringIntroResponseDto> createGatheringIntro(
            @RequestBody GatheringIntroRequestDto gatheringIntroRequestDto
    ) {
        GatheringIntroResponseDto gatheringIntroResponseDto =
                gatheringService.createGatheringIntro(gatheringIntroRequestDto);
        return ResponseEntity.ok(gatheringIntroResponseDto);
    }
}