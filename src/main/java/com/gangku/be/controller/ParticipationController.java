// src/main/java/com/gangku/be/controller/ParticipationController.java
package com.gangku.be.controller;

import com.gangku.be.domain.User;
import com.gangku.be.dto.gathering.response.ParticipantsPreviewDto;
import com.gangku.be.dto.participation.ParticipationResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.ErrorCode;
import com.gangku.be.service.ParticipationService;
import com.gangku.be.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/gatherings/{gatheringId}/participants")
public class ParticipationController {

    private final UserService userService;
    private final ParticipationService participationService;

    @PostMapping
    public ResponseEntity<ParticipationResponseDto> joinGathering(
//            @PathVariable Long gatheringId,
            @PathVariable Long gatheringId,
            @AuthenticationPrincipal User user

    ) {
        Long userId = user.getId();
//        User user = userService.findByUserId(userId); // 없으면 USER_NOT_FOUND 던짐
        ParticipationResponseDto responseDto = participationService.join(gatheringId, user);
        return ResponseEntity.ok(responseDto);
    }


    @DeleteMapping()
    public ResponseEntity<Void> cancelParticipation(
            @PathVariable Long gatheringId,
            @AuthenticationPrincipal User user
//            @AuthenticationPrincipal Long requesterId // 실제 요청자 (토큰에서 추출)
    ) {
        // 자기 자신 취소만 가능하거나 관리자 로직 등을 추가할 수도 있음
//        if (!userId.equals(requesterId)) {
//            throw new CustomException(ErrorCode.FORBIDDEN);
//        }

        participationService.cancelParticipation(gatheringId, user.getId());
        return ResponseEntity.noContent().build(); // 204 No Content
    }

     /**
         * [참여자 전체 목록 조회]
         * - /api/v1/gatherings/{gatheringId}/participants?page=1&size=3&sort=joinedAt,asc
         * - 캐러셀(좌우 이동)에 따라 페이징 처리됨
         */
    @GetMapping
    public ResponseEntity<ParticipantsPreviewDto> getParticipants(
            @PathVariable("gatheringId") Long gatheringId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "joinedAt,asc") String sort
    ) {
        ParticipantsPreviewDto response = participationService.getParticipants(gatheringId, page, size, sort);
        return ResponseEntity.ok(response);
    }
}