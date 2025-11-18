package com.gangku.be.util;


import com.gangku.be.domain.Category;
import com.gangku.be.dto.gathering.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.GatheringUpdateRequestDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.CustomExceptionOld;
import com.gangku.be.exception.ErrorCode;
import com.gangku.be.exception.ErrorCodeOld;
import com.gangku.be.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GatheringValidator {

    private final CategoryRepository categoryRepository;

    // 모임 생성 필드 검증 메서드
    public void validateFields(GatheringCreateRequestDto request) {
        // title: 1~30자
        String title = request.getTitle();
        if (title == null || title.length() < 1 || title.length() > 30) {
            throw new CustomExceptionOld(ErrorCodeOld.INVALID_FIELD_VALUE);
        }

        // imageUrl: URL 형식일 경우만 검사
        String imageUrl = request.getImageUrl();
        if (imageUrl != null && !ValidationUtil.isValidUrl(imageUrl)) {
            throw new CustomExceptionOld(ErrorCodeOld.INVALID_FIELD_VALUE);
        }

        // category: DB에서 조회된 것 중 하나여야 함
        List<String> allowed = categoryRepository.findAll().stream()
                .map(Category::getName)
                .collect(Collectors.toList());
        if (!allowed.contains(request.getCategory())) {
            throw new CustomExceptionOld(ErrorCodeOld.INVALID_FIELD_VALUE);
        }

        // capacity: 1~100
        int capacity = request.getCapacity();
        if (capacity < 1 || capacity > 100) {
            throw new CustomExceptionOld(ErrorCodeOld.INVALID_FIELD_VALUE);
        }

        // date: ISO 8601, 과거 불가
        if (request.getDate() == null || request.getDate().isBefore(LocalDateTime.now())) {
            throw new CustomExceptionOld(ErrorCodeOld.INVALID_FIELD_VALUE);
        }

        // location: 1~30자
        String location = request.getLocation();
        if (location == null || location.length() < 1 || location.length() > 30) {
            throw new CustomExceptionOld(ErrorCodeOld.INVALID_FIELD_VALUE);
        }

        // openChatUrl: https로 시작
        String chatUrl = request.getOpenChatUrl();
        if (chatUrl == null || !chatUrl.startsWith("https://")) {
            throw new CustomExceptionOld(ErrorCodeOld.INVALID_FIELD_VALUE);
        }

        // description: 최대 800자
        String desc = request.getDescription();
        if (desc == null || desc.length() > 800) {
            throw new CustomExceptionOld(ErrorCodeOld.INVALID_FIELD_VALUE);
        }
    }


    // 모임 수정 필드 검증 메서드
    public void validateFields(GatheringUpdateRequestDto request) {
        // title: 1~30자
        String title = request.getTitle();
        if (title == null || title.length() < 1 || title.length() > 30) {
            throw new CustomExceptionOld(ErrorCodeOld.INVALID_FIELD_VALUE);
        }

        // imageUrl: URL 형식일 경우만 검사
        String imageUrl = request.getImageUrl();
        if (imageUrl != null && !ValidationUtil.isValidUrl(imageUrl)) {
            throw new CustomExceptionOld(ErrorCodeOld.INVALID_FIELD_VALUE);
        }

        // category: DB에서 조회된 것 중 하나여야 함
        List<String> allowed = categoryRepository.findAll().stream()
                .map(Category::getName)
                .collect(Collectors.toList());
        if (!allowed.contains(request.getCategory())) {
            throw new CustomExceptionOld(ErrorCodeOld.INVALID_FIELD_VALUE);
        }

        // capacity: 1~100
        int capacity = request.getCapacity();
        if (capacity < 1 || capacity > 100) {
            throw new CustomExceptionOld(ErrorCodeOld.INVALID_FIELD_VALUE);
        }

        // date: ISO 8601, 과거 불가
        if (request.getDate() == null || request.getDate().isBefore(LocalDateTime.now())) {
            throw new CustomExceptionOld(ErrorCodeOld.INVALID_FIELD_VALUE);
        }

        // location: 1~30자
        String location = request.getLocation();
        if (location == null || location.length() < 1 || location.length() > 30) {
            throw new CustomExceptionOld(ErrorCodeOld.INVALID_FIELD_VALUE);
        }

        // openChatUrl: https로 시작
        String chatUrl = request.getOpenChatUrl();
        if (chatUrl == null || !chatUrl.startsWith("https://")) {
            throw new CustomExceptionOld(ErrorCodeOld.INVALID_FIELD_VALUE);
        }

        // description: 최대 800자
        String desc = request.getDescription();
        if (desc == null || desc.length() > 800) {
            throw new CustomExceptionOld(ErrorCodeOld.INVALID_FIELD_VALUE);
        }
    }
}
