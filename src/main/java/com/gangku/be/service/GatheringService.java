package com.gangku.be.service;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import com.gangku.be.dto.common.PageMetaDto;
import com.gangku.be.dto.gathering.request.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.request.GatheringUpdateRequestDto;
import com.gangku.be.dto.gathering.response.*;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.ErrorCode;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.util.GatheringValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class GatheringService {

    private final GatheringRepository gatheringRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRepository participationRepository;
    private final GatheringValidator gatheringValidator;



    /**
     *   모임 상세 조회
     * - 특정 모임 ID로 상세 정보 조회
     * - 참여자 미리보기(3명) + 참여자 총원(meta) 포함
     * - 상세 화면에서 캐러셀 형태로 사용됨
     */
    @Transactional(readOnly = true)
    public GatheringDetailResponseDto getGatheringById(Long gatheringId, Long userId) {

        if (gatheringId == null || gatheringId <= 0) {
            throw new CustomException(ErrorCode.INVALID_GATHERING_ID);
        }

        Gathering gathering = gatheringRepository.findById(gatheringId)
                .orElseThrow(() -> new CustomException(ErrorCode.GATHERING_NOT_FOUND));

        // 참여자 3명 미리보기 + 전체 인원수 계산
        List<Participation> participants = participationRepository.findTop3ByGatheringOrderByJoinedAtAsc(gathering);
        long totalElements = participationRepository.countByGathering(gathering);

        // meta 정보 생성 , 현재는 페이지 1, 사이즈 3 고정
        PageMetaDto meta = PageMetaDto.of(1, 3, totalElements, "joinedAt,asc");

        //참여자 DTO 반환
        List<ParticipantPreviewDto> previews = participants.stream()
                .map(ParticipantPreviewDto::from)
                .collect(Collectors.toList());

        // Gathering + ParticipantsPreview 반환
        return GatheringDetailResponseDto.from(gathering, previews, meta);
    }

    /**
     *   모임 목록 조회
     * - 홈 화면 및 카테고리 페이지에서 사용
     * - category, sort, size에 따라 정렬 및 필터링
     *   - sort = latest → createdAt DESC
     *   - sort = popular → participantCount DESC
     */
    @Transactional(readOnly = true)
    public GatheringListResponseDto getGatheringList(String categoryName, String sort, int size) {
        if (size <= 0 || size > 12) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER_FORMAT);
        }
        Category category = null;
        if (categoryName != null) {
            category = categoryRepository.findByName(categoryName)
                    .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));
        }
        Pageable pageable = PageRequest.of(0, size);
        // 정렬 조건에 따라 조회
        List<Gathering> gatherings;
        if (sort.equals("popular")) {
            gatherings = gatheringRepository.findPopularGatherings(category, pageable);
        } else {
            gatherings = gatheringRepository.findLatestGatherings(category, pageable);
        }
        // 엔티티 -> Dto 변환
        List<GatheringListItemDto> items = gatherings.stream()
                .map(g -> GatheringListItemDto.builder()
                        .id("gath_" + g.getId())
                        .imageUrl(g.getImageUrl())
                        .category(g.getCategory().getName())
                        .title(g.getTitle())
                        .hostName(g.getHost().getNickname())
                        .participantCount(g.getParticipantCount())
                        .capacity(g.getCapacity())
                        .build())
                .toList();

        PageMetaDto meta = PageMetaDto.builder()
                .size(items.size())
                .sortedBy(sort.equals("popular") ? "participantCount,desc" : "createdAt,desc")
                .nextCursor(null) // TODO: 커서 기반 페이지네이션 추가 시 구현
                .hasPrev(false)
                .hasNext(false)
                .build();

        return GatheringListResponseDto.builder()
                .data(items)
                .meta(meta)
                .build();
    }

    //모임 생성 메서드
    @Transactional
    public GatheringCreateResponseDto createGathering(GatheringCreateRequestDto request, User host) {

        //필드 유효성 검사 예외처리
        gatheringValidator.validateFields(request);

        // 401 Unauthorized 예외처리는 JwtAuthFilter에서 처리함

        // 카테고리 유효성 검사
        Category category = categoryRepository.findByName(request.getCategory())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 카테고리입니다."));


        // 엔티티 생성
        Gathering gathering = Gathering.builder()
                .host(host)
                .title(request.getTitle())
                .imageUrl(request.getImageUrl())
                .category(category)
                .capacity(request.getCapacity())
                .participantCount(0)
                .date(request.getDate())
                .location(request.getLocation())
                .openChatUrl(request.getOpenChatUrl())
                .description(request.getDescription())
                .status(Gathering.Status.RECRUITING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Gathering savedGathering = gatheringRepository.save(gathering);

        // 호스트도 참여자로 추가
        Participation participation = Participation.builder()
                .user(host)
                .gathering(savedGathering)
                .status(Participation.Status.APPROVED)
                .role(Participation.ParticipationRole.HOST)
                .build();
        participationRepository.save(participation);

        savedGathering.setParticipantCount(savedGathering.getParticipantCount() + 1);
        gatheringRepository.save(savedGathering); // 업데이트 반영

        // 4. 응답 DTO 생성
        return GatheringCreateResponseDto.from(savedGathering);
    }


    // 모임 수정 메서드
    @Transactional
    public GatheringUpdateResponseDto updateGathering(Long gatheringId, Long userId, GatheringUpdateRequestDto request) {

        // 필드 유효성 검사 예외처리
        gatheringValidator.validateFields(request);

        // 401 Unauthorized 예외처리는 JwtAuthFilter에서 처리함

        Gathering gathering = gatheringRepository.findById(gatheringId)
                .orElseThrow(() -> new CustomException(ErrorCode.GATHERING_NOT_FOUND));

        if (!gathering.getHost().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        if (request.getTitle() != null) gathering.setTitle(request.getTitle());
        if (request.getImageUrl() != null) gathering.setImageUrl(request.getImageUrl());
        if (request.getCategory() != null) {
            Category category = categoryRepository.findByName(request.getCategory())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
            gathering.setCategory(category);
        }
        if (request.getCapacity() != null) gathering.setCapacity(request.getCapacity());
        if (request.getDate() != null) gathering.setDate(request.getDate());
        if (request.getLocation() != null) gathering.setLocation(request.getLocation());
        if (request.getOpenChatUrl() != null) gathering.setOpenChatUrl(request.getOpenChatUrl());
        if (request.getDescription() != null) gathering.setDescription(request.getDescription());

        Gathering updated = gatheringRepository.save(gathering);

        return GatheringUpdateResponseDto.builder()
                .id("gath_" + updated.getId())
                .title(updated.getTitle())
                .imageUrl(updated.getImageUrl())
                .category(updated.getCategory().getName())
                .capacity(updated.getCapacity())
                .date(updated.getDate())
                .location(updated.getLocation())
                .openChatUrl(updated.getOpenChatUrl())
                .description(updated.getDescription())
                .updatedAt(updated.getUpdatedAt().toString())
                .build();
    }

    // 모임 삭제 메서드
    @Transactional
    public void deleteGathering(Long gatheringId, Long userId) {

        // 400 Bad Request는 GlobalExceptionHandler 에서 처리

        Gathering gathering = gatheringRepository.findById(gatheringId)
                .orElseThrow(() -> new CustomException(ErrorCode.GATHERING_NOT_FOUND));

        if (!gathering.getHost().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "해당 모임을 삭제할 권한이 없습니다.");
        }

        gatheringRepository.delete(gathering);
    }

    public Gathering getGatheringById(Long id) {
        return gatheringRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.GATHERING_NOT_FOUND));
        // 401 Unauthorized 예외처리는 JwtAuthFilter에서 처리함
        // 400 Bad Request는 GlobalExceptionHandler 에서 처리
    }


}