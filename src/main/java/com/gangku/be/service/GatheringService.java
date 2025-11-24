package com.gangku.be.service;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import com.gangku.be.dto.gathering.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.GatheringResponseDto;
import com.gangku.be.dto.gathering.GatheringUpdateRequestDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.AuthErrorCode;
import com.gangku.be.exception.constant.CategoryErrorCode;
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class GatheringService {

    private final GatheringRepository gatheringRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;

    //모임 생성 메서드
    @Transactional
    public GatheringResponseDto createGathering(
            GatheringCreateRequestDto gatheringCreateRequestDto,
            Long hostId
    ) {

        //필드 유효성 검사 예외처리
        validateFields(gatheringCreateRequestDto);

        User host = findUserById(hostId);

        Category category = findCategoryByName(gatheringCreateRequestDto.getCategory());

        // 엔티티 생성
        Gathering gathering = Gathering.create(
                host,
                category,
                gatheringCreateRequestDto.getTitle(),
                gatheringCreateRequestDto.getDescription(),
                gatheringCreateRequestDto.getImageUrl(),
                gatheringCreateRequestDto.getCapacity(),
                gatheringCreateRequestDto.getDate(),
                gatheringCreateRequestDto.getLocation(),
                gatheringCreateRequestDto.getOpenChatUrl()
        );
        Gathering savedGathering = gatheringRepository.save(gathering);

        // 호스트도 참여자로 추가
        Participation participation = Participation.create(host, savedGathering);
        participationRepository.save(participation);

        // 4. 응답 DTO 생성
        return GatheringResponseDto.from(savedGathering);
    }

    // 모임 수정 메서드
    @Transactional
    public GatheringResponseDto updateGathering(
            Long gatheringId,
            Long userId,
            GatheringUpdateRequestDto gatheringUpdateRequestDto
    ) {

        // 필드 유효성 검사 예외처리
        validateFields(gatheringUpdateRequestDto);

        Gathering gathering = findGatheringById(gatheringId);

        validateGatheringHost(userId, gathering);

        checkRequestBodyAndUpdate(gatheringUpdateRequestDto, gathering);

        Gathering updatedGathering = gatheringRepository.save(gathering);

        return GatheringResponseDto.from(updatedGathering);
    }

    // 모임 삭제 메서드
    @Transactional
    public void deleteGathering(Long gatheringId, Long userId) {

        Gathering gathering = findGatheringById(gatheringId);   

        validateGatheringHost(userId, gathering);

        gatheringRepository.delete(gathering);
    }

    private User findUserById(Long hostId) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
        return host;
    }

    private Category findCategoryByName(String categoryName) {
        return categoryRepository.findByName(categoryName)
                .orElseThrow(() -> new CustomException(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }

    public Gathering findGatheringById(Long gatheringId) {
        return gatheringRepository.findById(gatheringId)
                .orElseThrow(() -> new CustomException(GatheringErrorCode.GATHERING_NOT_FOUND));
    }

    private void checkRequestBodyAndUpdate(GatheringUpdateRequestDto gatheringUpdateRequestDto, Gathering gathering) {
        if (gatheringUpdateRequestDto.getTitle() != null) gathering.setTitle(gatheringUpdateRequestDto.getTitle());
        if (gatheringUpdateRequestDto.getImageUrl() != null) gathering.setImageUrl(gatheringUpdateRequestDto.getImageUrl());
        if (gatheringUpdateRequestDto.getCategory() != null) gathering.setCategory(findCategoryByName(
                gatheringUpdateRequestDto.getCategory()));
        if (gatheringUpdateRequestDto.getCapacity() != null) gathering.setCapacity(gatheringUpdateRequestDto.getCapacity());
        if (gatheringUpdateRequestDto.getDate() != null) gathering.setDate(gatheringUpdateRequestDto.getDate());
        if (gatheringUpdateRequestDto.getLocation() != null) gathering.setLocation(gatheringUpdateRequestDto.getLocation());
        if (gatheringUpdateRequestDto.getOpenChatUrl() != null) gathering.setOpenChatUrl(gatheringUpdateRequestDto.getOpenChatUrl());
        if (gatheringUpdateRequestDto.getDescription() != null) gathering.setDescription(gatheringUpdateRequestDto.getDescription());
    }

    private void validateGatheringHost(Long userId, Gathering gathering) {
        if (!gathering.getHost().getId().equals(userId)) {
            throw new CustomException(GatheringErrorCode.FORBIDDEN);
        }
    }

    private boolean isValidUrl(String url) {
        return UrlValidator.getInstance().isValid(url);
    }

    private void validateFields(GatheringCreateRequestDto request) {
        // title: 1~30자
        String title = request.getTitle();
        if (title == null || title.length() < 1 || title.length() > 30) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // imageUrl: URL 형식일 경우만 검사
        String imageUrl = request.getImageUrl();
        if (imageUrl != null && !isValidUrl(imageUrl)) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // category: DB에서 조회된 것 중 하나여야 함
        List<String> allowed = categoryRepository.findAll().stream()
                .map(Category::getName)
                .collect(Collectors.toList());
        if (!allowed.contains(request.getCategory())) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // capacity: 1~100
        int capacity = request.getCapacity();
        if (capacity < 1 || capacity > 100) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // date: ISO 8601, 과거 불가
        if (request.getDate() == null || request.getDate().isBefore(LocalDateTime.now())) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // location: 1~30자
        String location = request.getLocation();
        if (location == null || location.length() < 1 || location.length() > 30) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // openChatUrl: https로 시작
        String chatUrl = request.getOpenChatUrl();
        if (chatUrl == null || !chatUrl.startsWith("https://")) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // description: 최대 800자
        String desc = request.getDescription();
        if (desc == null || desc.length() > 800) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }
    }


    // 모임 수정 필드 검증 메서드
    private void validateFields(GatheringUpdateRequestDto request) {
        // title: 1~30자
        String title = request.getTitle();
        if (title == null || title.length() < 1 || title.length() > 30) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // imageUrl: URL 형식일 경우만 검사
        String imageUrl = request.getImageUrl();
        if (imageUrl != null && !isValidUrl(imageUrl)) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // category: DB에서 조회된 것 중 하나여야 함
        List<String> allowed = categoryRepository.findAll().stream()
                .map(Category::getName)
                .collect(Collectors.toList());
        if (!allowed.contains(request.getCategory())) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // capacity: 1~100
        int capacity = request.getCapacity();
        if (capacity < 1 || capacity > 100) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // date: ISO 8601, 과거 불가
        if (request.getDate() == null || request.getDate().isBefore(LocalDateTime.now())) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // location: 1~30자
        String location = request.getLocation();
        if (location == null || location.length() < 1 || location.length() > 30) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // openChatUrl: https로 시작
        String chatUrl = request.getOpenChatUrl();
        if (chatUrl == null || !chatUrl.startsWith("https://")) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // description: 최대 800자
        String desc = request.getDescription();
        if (desc == null || desc.length() > 800) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }
    }
}