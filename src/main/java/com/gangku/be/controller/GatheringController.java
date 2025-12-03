package com.gangku.be.controller;

import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.dto.gathering.request.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.request.GatheringUpdateRequestDto;
import com.gangku.be.dto.gathering.request.GatheringIntroRequestDto;
import com.gangku.be.dto.gathering.response.GatheringDetailResponseDto;
import com.gangku.be.dto.gathering.response.GatheringIntroResponseDto;
import com.gangku.be.dto.gathering.response.GatheringListResponseDto;
import com.gangku.be.dto.gathering.response.GatheringResponseDto;
import com.gangku.be.model.PrefixedId;
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
            @AuthenticationPrincipal(expression = "id") Long userId
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
            @PathVariable String gatheringId,
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestBody GatheringUpdateRequestDto gatheringUpdateRequestDto
    ) {
        Long internalGatheringId = PrefixedId.parse(gatheringId).require(ResourceType.GATHERING);
        GatheringResponseDto gatheringResponseDto =
                gatheringService.updateGathering(internalGatheringId, userId, gatheringUpdateRequestDto);
        return ResponseEntity.ok(gatheringResponseDto);
    }

    @GetMapping("/{gatheringId}")
    public ResponseEntity<GatheringDetailResponseDto> getGatheringDetail(
            @PathVariable String gatheringId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "joinedAt,asc") String sort
    ) {
        Long internalGatheringId = PrefixedId.parse(gatheringId).require(ResourceType.GATHERING);

        GatheringDetailResponseDto gatheringDetailResponseDto =
                gatheringService.getGatheringDetail(internalGatheringId, page, size, sort);
        return ResponseEntity.ok(gatheringDetailResponseDto);
    }

    // 모임 삭제
    @DeleteMapping("/{gatheringId}")
    public ResponseEntity<Void> deleteGathering(
            @PathVariable String gatheringId,
            @AuthenticationPrincipal(expression = "id") Long userId
    ) {
        Long internalGatheringId = PrefixedId.parse(gatheringId).require(ResourceType.GATHERING);

        gatheringService.deleteGathering(internalGatheringId, userId);
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

    // 모임 리스트 조회
    // 카테고리 페이지에서 사용
    @GetMapping
    public ResponseEntity<GatheringListResponseDto> getGatheringList(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        GatheringListResponseDto gatheringListResponseDto =
                gatheringService.getGatheringList(category, page, size, sort);
        return ResponseEntity.ok(gatheringListResponseDto);
    }

    // 홈 화면 모임 리스트 조회
    @GetMapping("/api/v1/home")
    public ResponseEntity<Map<String, GatheringListResponseDto>> getHomeGatherings() {
        /*
        이거 홈화면 추천 모임 보내는거
         */
        Map<String, GatheringListResponseDto> result = new HashMap<>();

        result.put("recommended", gatheringService.getGatheringList(null, 1, 3, "latest")); // Recommend 로직 이후에 추가 임시
        result.put("latest", gatheringService.getGatheringList(null, 1, 3, "latest"));
        result.put("popular", gatheringService.getGatheringList(null, 1, 3, "popular"));

        return ResponseEntity.ok(result);
    }
}