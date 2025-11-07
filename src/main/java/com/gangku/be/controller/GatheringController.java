// src/main/java/com/gangku/be/controller/GatheringController.java

package com.gangku.be.controller;

import com.gangku.be.domain.User;
import com.gangku.be.dto.gathering.request.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.response.GatheringCreateResponseDto;
import com.gangku.be.dto.gathering.request.GatheringUpdateRequestDto;
import com.gangku.be.dto.gathering.response.GatheringDetailResponseDto;
import com.gangku.be.dto.gathering.response.GatheringListResponseDto;
import com.gangku.be.dto.gathering.response.GatheringUpdateResponseDto;
import com.gangku.be.service.GatheringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/gatherings")
@RequiredArgsConstructor
public class GatheringController {

    private final GatheringService gatheringService;

    // 모임 생성
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

    // 모임 수정
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

    // 모임의 상세정보 + 참여자 미리보기(3명) + meta 정보 반환
    @GetMapping("/{gatheringId}")
    public ResponseEntity<GatheringDetailResponseDto> getGatheringById(
            @PathVariable Long gatheringId,
            @AuthenticationPrincipal Long userId
    ) {
        GatheringDetailResponseDto response = gatheringService.getGatheringById(gatheringId, userId);
        return ResponseEntity.ok(response);
    }

    // 모임 리스트 조회
    // 카테고리 페이지에서 사용
    @GetMapping
    public ResponseEntity<GatheringListResponseDto> getGatherings(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(required = false) String cursor
    ) {
        GatheringListResponseDto response = gatheringService.getGatheringList(category, sort, size);
        return ResponseEntity.ok(response);
    }

    // 홈 화면 모임 리스트 조회
    @GetMapping("/api/v1/home")
    public ResponseEntity<Map<String, GatheringListResponseDto>> getHomeGatherings() {
        Map<String, GatheringListResponseDto> result = new HashMap<>();

        result.put("recommended", gatheringService.getGatheringList(null, "recommended", 3));
        result.put("latest", gatheringService.getGatheringList(null, "latest", 3));
        result.put("popular", gatheringService.getGatheringList(null, "popular", 3));

        return ResponseEntity.ok(result);
    }
}