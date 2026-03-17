package com.gangku.be.controller;

import com.gangku.be.constant.id.ResourceType;
import com.gangku.be.domain.User;
import com.gangku.be.dto.gathering.response.GatheringListResponseDto;
import com.gangku.be.dto.review.ReviewListResponseDto;
import com.gangku.be.dto.user.*;
import com.gangku.be.model.common.PrefixedId;
import com.gangku.be.service.GatheringService;
import com.gangku.be.service.UserService;
import com.gangku.be.util.object.FileUrlResolver;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Validated
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final GatheringService gatheringService;
    private final FileUrlResolver fileUrlResolver;

    @PostMapping
    public ResponseEntity<SignUpResponseDto> registerUser(
            @RequestBody @Valid SignUpRequestDto signUpRequestDto,
            @CookieValue(value = "signup_session", required = false) String sessionId) {

        // 1) 회원가입 처리 후 저장된 유저 반환
        User newUser = userService.registerUser(signUpRequestDto, sessionId);
        String imageUrl = fileUrlResolver.toPublicUrl(signUpRequestDto.getProfileImageObjectKey());

        // 2) Location 헤더 설정을 위한 정보 저장
        URI location =
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(newUser.getId())
                        .toUri();

        return ResponseEntity.created(location).body(SignUpResponseDto.from(newUser, imageUrl));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable("userId") String targetUserId,
            @AuthenticationPrincipal Long currentUserId) {
        Long internalTargetUserId = PrefixedId.parse(targetUserId).require(ResourceType.USER);

        userService.deleteUser(internalTargetUserId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/review")
    public ResponseEntity<UpdateReviewSettingResponseDto> updateReviewSetting(
            @PathVariable("userId") String targetUserId,
            @AuthenticationPrincipal Long currentUserId,
            @RequestBody @Valid UpdateReviewSettingRequestDto updateReviewSettingRequestDto) {
        Long internalTargetUserId = PrefixedId.parse(targetUserId).require(ResourceType.USER);

        UpdateReviewSettingResponseDto updateReviewSettingResponseDto =
                userService.updateReviewSetting(
                        internalTargetUserId,
                        currentUserId,
                        updateReviewSettingRequestDto.getReviewPublic());
        return ResponseEntity.ok(updateReviewSettingResponseDto);
    }

    /** 특정 사용자의 모임 목록 조회 - role=host → 내가 만든 모임 - role=guest → 내가 참여한 모임 */
    @GetMapping("/gatherings")
    public ResponseEntity<GatheringListResponseDto> getUserGatherings(
            @AuthenticationPrincipal Long userId,
            @RequestParam @Pattern(regexp = "^(host|guest)") String role,
            @RequestParam(defaultValue = "1") @Min(value = 1) int page,
            @RequestParam(defaultValue = "10") @Min(value = 1) @Max(value = 50) int size) {
        GatheringListResponseDto response =
                gatheringService.getUserGatherings(userId, role, page, size);
        return ResponseEntity.ok().header("Cache-Control", "private, max-age=60").body(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponseDto> getUserProfile(@PathVariable String userId) {
        Long internalUserId = PrefixedId.parse(userId).require(ResourceType.USER);
        UserProfileResponseDto response = userService.getUserProfile(internalUserId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserProfileUpdateResponseDto> updateUserProfile(
            @PathVariable String userId,
            @AuthenticationPrincipal Long currentUserId,
            @Valid @RequestBody UserProfileUpdateRequestDto requestDto) {
        Long internalUserId = PrefixedId.parse(userId).require(ResourceType.USER);
        UserProfileUpdateResponseDto response =
                userService.updateUserProfile(internalUserId, currentUserId, requestDto);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/reviews")
    public ResponseEntity<ReviewListResponseDto> getUserReviews(
            @PathVariable String userId,
            @AuthenticationPrincipal Long currentUserId,
            @RequestParam(defaultValue = "3") @Min(1) @Max(5) int size,
            @RequestParam(required = false) String cursor) {
        Long internalUserId = PrefixedId.parse(userId).require(ResourceType.USER);

        ReviewListResponseDto response =
                userService.getUserReviews(internalUserId, currentUserId, size, cursor);

        return ResponseEntity.ok(response);
    }
}
