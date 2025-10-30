package com.gangku.be.controller;

import com.gangku.be.domain.User;
import com.gangku.be.dto.common.DataResponseDto;
import com.gangku.be.dto.preferred.PreferredCategoryRequestDto;
import com.gangku.be.dto.preferred.PreferredCategoryResponseDto;
import com.gangku.be.service.PreferredCategoryService;
import com.gangku.be.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/preferred-categories")
@RequiredArgsConstructor
public class PreferredCategoryController {


    private final PreferredCategoryService preferredCategoryService;
    private final UserService userService;

    /**
     * [POST] 사용자 선호 카테고리 설정
     * 요청 바디에 category 이름 리스트를 받고,
     * 현재 로그인한 사용자에게 선호 카테고리로 저장한다.
     */
    @PostMapping
    public ResponseEntity<DataResponseDto<PreferredCategoryResponseDto>> setPreferredCategories(
            @RequestBody PreferredCategoryRequestDto requestDto,
            Principal principal
    ) {
        // 🔐 인증된 사용자의 ID 추출
        Long userId = Long.parseLong(principal.getName());

        // 🧑 사용자 조회
        User user = userService.findByUserId(userId);

        PreferredCategoryResponseDto response =
                preferredCategoryService.setPreferredCategories(user, requestDto.getCategoryNames());

        return ResponseEntity.ok(new DataResponseDto<>(response));
    }

    /**
     * [GET] 사용자 선호 카테고리 조회
     * 현재 로그인한 사용자의 선호 카테고리 목록을 반환한다.
     */
    @GetMapping
    public ResponseEntity<DataResponseDto<PreferredCategoryResponseDto>> getPreferredCategories(
            Principal principal
    ) {
        // 🔐 인증된 사용자 ID 추출
        Long userId = Long.parseLong(principal.getName());

        // 📦 선호 카테고리 이름 리스트 조회
        List<String> preferredNames = preferredCategoryService.getPreferredCategoryNames(userId);

        // DTO로 래핑
        PreferredCategoryResponseDto responseDto = new PreferredCategoryResponseDto(preferredNames);

        return ResponseEntity.ok(new DataResponseDto<>(responseDto));
    }
}