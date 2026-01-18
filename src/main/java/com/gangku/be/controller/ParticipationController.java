package com.gangku.be.controller;

import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.dto.participation.ParticipantsPreviewResponseDto;
import com.gangku.be.dto.participation.ParticipationResponseDto;
import com.gangku.be.model.common.PrefixedId;
import com.gangku.be.service.ParticipationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/gatherings/{gatheringId}/participants")
public class ParticipationController {

    private final ParticipationService participationService;

    @PostMapping()
    public ResponseEntity<ParticipationResponseDto> joinParticipation(
            @PathVariable("gatheringId") String gatheringId,
            @AuthenticationPrincipal Long userId
    ) {
        Long internalGatheringId = PrefixedId.parse(gatheringId).require(ResourceType.GATHERING);

        return ResponseEntity.ok(participationService.joinParticipation(internalGatheringId, userId));
    }

    @DeleteMapping()
    public ResponseEntity<Void> cancelParticipation(
            @PathVariable String gatheringId,
            @AuthenticationPrincipal Long userId
    ) {
        log.info("[CANCEL_PARTICIPATION] [Controller] enter gatheringId={}, userId={}", gatheringId, userId);
        Long internalGatheringId = PrefixedId.parse(gatheringId).require(ResourceType.GATHERING);

        participationService.cancelParticipation(internalGatheringId, userId);
        log.info("[CANCEL_PARTICIPATION][Controller] success gatheringId={}, userId={}", gatheringId, userId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @GetMapping()
    public ResponseEntity<ParticipantsPreviewResponseDto> getParticipants(
            @PathVariable String gatheringId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "joinedAt,asc") String sort
    ) {
        Long internalGatheringId = PrefixedId.parse(gatheringId).require(ResourceType.GATHERING);

        ParticipantsPreviewResponseDto participantsPreviewResponseDto =
                participationService.getParticipants(internalGatheringId, page, size, sort);

        return ResponseEntity.ok(participantsPreviewResponseDto);
    }
}