package com.gangku.be.controller;

import com.gangku.be.constant.gathering.GatheringSort;
import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.dto.ai.request.IntroCreateRequestDto;
import com.gangku.be.dto.ai.response.IntroCreateResponseDto;
import com.gangku.be.dto.gathering.request.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.request.GatheringUpdateRequestDto;
import com.gangku.be.dto.gathering.response.GatheringDetailResponseDto;
import com.gangku.be.dto.gathering.response.GatheringListResponseDto;
import com.gangku.be.dto.gathering.response.GatheringResponseDto;
import com.gangku.be.model.common.PrefixedId;
import com.gangku.be.service.GatheringService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Validated
@RequestMapping("/api/v1/gatherings")
@RequiredArgsConstructor
public class GatheringController {

    private final GatheringService gatheringService;

    @PostMapping
    public ResponseEntity<GatheringResponseDto> createGathering(
            @RequestBody @Valid GatheringCreateRequestDto gatheringCreateRequestDto,
            @AuthenticationPrincipal Long userId) {

        // 모임 생성
        GatheringResponseDto gatheringCreateResponseDto =
                gatheringService.createGathering(gatheringCreateRequestDto, userId);

        // 응답 구성
        URI location =
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(gatheringCreateResponseDto.getId())
                        .toUri();

        return ResponseEntity.created(location).body(gatheringCreateResponseDto);
    }

    @PatchMapping("/{gatheringId}")
    public ResponseEntity<GatheringResponseDto> updateGathering(
            @PathVariable String gatheringId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid GatheringUpdateRequestDto gatheringUpdateRequestDto) {
        Long internalGatheringId = PrefixedId.parse(gatheringId).require(ResourceType.GATHERING);
        GatheringResponseDto gatheringResponseDto =
                gatheringService.updateGathering(
                        internalGatheringId, userId, gatheringUpdateRequestDto);
        return ResponseEntity.ok(gatheringResponseDto);
    }

    @GetMapping("/{gatheringId}")
    public ResponseEntity<GatheringDetailResponseDto> getGatheringDetail(
            @PathVariable String gatheringId,
            @RequestParam(defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(defaultValue = "3") @Min(value = 1) @Max(value = 10) int size,
            @AuthenticationPrincipal Long userId) {
        Long internalGatheringId = PrefixedId.parse(gatheringId).require(ResourceType.GATHERING);

        GatheringDetailResponseDto gatheringDetailResponseDto =
                gatheringService.getGatheringDetail(internalGatheringId, page, size, userId);
        return ResponseEntity.ok(gatheringDetailResponseDto);
    }

    // 모임 삭제
    @DeleteMapping("/{gatheringId}")
    public ResponseEntity<Void> deleteGathering(
            @PathVariable String gatheringId, @AuthenticationPrincipal Long userId) {
        Long internalGatheringId = PrefixedId.parse(gatheringId).require(ResourceType.GATHERING);

        gatheringService.deleteGathering(internalGatheringId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{gatheringId}/finish")
    public ResponseEntity<Void> finishGathering(
            @PathVariable String gatheringId, @AuthenticationPrincipal Long userId) {
        Long internalGatheringId = PrefixedId.parse(gatheringId).require(ResourceType.GATHERING);

        gatheringService.finishGathering(internalGatheringId, userId);
        return ResponseEntity.noContent().build();
    }

    // AI 모임 정보 생성
    @PostMapping("/intro")
    public ResponseEntity<IntroCreateResponseDto> createGatheringIntro(
            @RequestBody @Valid IntroCreateRequestDto introCreateRequestDto) {
        IntroCreateResponseDto introCreateResponseDto =
                gatheringService.createGatheringIntro(introCreateRequestDto);
        return ResponseEntity.ok(introCreateResponseDto);
    }

    @GetMapping
    public ResponseEntity<GatheringListResponseDto> getGatheringList(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(defaultValue = "3") @Max(value = 12) int size,
            @RequestParam(defaultValue = "latest") String sort) {
        GatheringListResponseDto gatheringListResponseDto =
                gatheringService.getGatheringList(userId, category, page, size, sort);
        return ResponseEntity.ok(gatheringListResponseDto);
    }
}
