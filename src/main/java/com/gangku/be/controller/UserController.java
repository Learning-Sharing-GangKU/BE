package com.gangku.be.controller;

import com.gangku.be.domain.User;
import com.gangku.be.dto.user.SignUpRequestDto;
import com.gangku.be.dto.user.SignUpResponseDto;
import com.gangku.be.dto.gathering.response.GatheringListResponseDto;
import com.gangku.be.service.GatheringService;
import com.gangku.be.service.UserService;
import com.gangku.be.util.object.FileUrlResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final GatheringService gatheringService;
    private final FileUrlResolver fileUrlResolver;

    @PostMapping
    public ResponseEntity<SignUpResponseDto> registerUser(@RequestBody @Valid SignUpRequestDto signUpRequestDto) {

        // 1) 회원가입 처리 후 저장된 유저 반환
        User newUser = userService.registerUser(signUpRequestDto);
        String imageUrl = fileUrlResolver.toPublicUrl(signUpRequestDto.getProfileImageObjectKey());

        // 2) Location 헤더 설정을 위한 정보 저장
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newUser.getId())
                .toUri();

        return ResponseEntity.created(location).body(SignUpResponseDto.from(newUser, imageUrl));
    }

    /**
     * 특정 사용자의 모임 목록 조회
     * - role=host → 내가 만든 모임
     * - role=guest → 내가 참여한 모임
     */
    @GetMapping("/gatherings")
    public ResponseEntity<GatheringListResponseDto> getUserGatherings(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @RequestParam String role,
            @RequestParam int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        GatheringListResponseDto response = gatheringService.getUserGatherings(userId, role, page, size, sort);
        return ResponseEntity.ok()
                .header("Cache-Control", "private, max-age=60")
                .body(response);
    }

}
