package com.gangku.be.controller;

import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.dto.participation.ParticipantsPreviewResponseDto;
import com.gangku.be.dto.participation.ParticipationResponseDto;
import com.gangku.be.model.common.PrefixedId;
import com.gangku.be.service.ParticipationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/gatherings/{gatheringId}/participants")
public class ParticipationController {

    private final ParticipationService participationService;

    @PostMapping()
    public ResponseEntity<ParticipationResponseDto> joinParticipation(
            @PathVariable String gatheringId,
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
        Long internalGatheringId = PrefixedId.parse(gatheringId).require(ResourceType.GATHERING);

        participationService.cancelParticipation(internalGatheringId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping()
    public ResponseEntity<ParticipantsPreviewResponseDto> getParticipants(
            @PathVariable String gatheringId,
            @RequestParam(defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(defaultValue = "3") @Max(value = 10) int size,
            @RequestParam(defaultValue = "joinedAt,asc") @Pattern(regexp = "^joinedAt.(asc|desc)$") String sort
    ) {
        Long internalGatheringId = PrefixedId.parse(gatheringId).require(ResourceType.GATHERING);

        ParticipantsPreviewResponseDto participantsPreviewResponseDto =
                participationService.getParticipants(internalGatheringId, page, size, sort);

        return ResponseEntity.ok(participantsPreviewResponseDto);
    }
}