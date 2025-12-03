package com.gangku.be.controller;

import com.gangku.be.dto.participation.ParticipantsPreviewResponseDto;
import com.gangku.be.dto.participation.ParticipationResponseDto;
import com.gangku.be.service.ParticipationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/gatherings/{gatheringId}/participants")
public class ParticipationController {

    private final ParticipationService participationService;

    @PostMapping()
    public ResponseEntity<ParticipationResponseDto> joinParticipation(
            @PathVariable("gatheringId") Long gatheringId,
            @AuthenticationPrincipal(expression = "id") Long userId
    ) {
        return ResponseEntity.ok(participationService.joinParticipation(gatheringId, userId));
    }

    @DeleteMapping()
    public ResponseEntity<Void> cancelParticipation(
            @PathVariable Long gatheringId,
            @AuthenticationPrincipal(expression = "id") Long userId // 실제 요청자 (토큰에서 추출)
    ) {
        participationService.cancelParticipation(gatheringId, userId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @GetMapping()
    public ResponseEntity<ParticipantsPreviewResponseDto> getParticipants(
            @PathVariable Long gatheringId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "joinedAt,asc") String sort
    ) {
        ParticipantsPreviewResponseDto participantsPreviewResponseDto =
                participationService.getParticipants(gatheringId, page, size, sort);

        return ResponseEntity.ok(participantsPreviewResponseDto);
    }
}