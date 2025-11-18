package com.gangku.be.service;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import com.gangku.be.dto.gathering.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.GatheringCreateResponseDto;
import com.gangku.be.dto.gathering.GatheringUpdateRequestDto;
import com.gangku.be.dto.gathering.GatheringUpdateResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.CustomExceptionOld;
import com.gangku.be.exception.ErrorCode;
import com.gangku.be.exception.ErrorCodeOld;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.util.GatheringValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class GatheringService {

    private final GatheringRepository gatheringRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRepository participationRepository;
    private final GatheringValidator gatheringValidator;


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
                .orElseThrow(() -> new CustomExceptionOld(ErrorCodeOld.GATHERING_NOT_FOUND));

        if (!gathering.getHost().getId().equals(userId)) {
            throw new CustomExceptionOld(ErrorCodeOld.FORBIDDEN);
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
                .orElseThrow(() -> new CustomExceptionOld(ErrorCodeOld.GATHERING_NOT_FOUND));

        if (!gathering.getHost().getId().equals(userId)) {
            throw new CustomExceptionOld(ErrorCodeOld.FORBIDDEN, "해당 모임을 삭제할 권한이 없습니다.");
        }

        gatheringRepository.delete(gathering);
    }

    public Gathering getGatheringById(Long id) {
        return gatheringRepository.findById(id)
                .orElseThrow(() -> new CustomExceptionOld(ErrorCodeOld.GATHERING_NOT_FOUND));
        // 401 Unauthorized 예외처리는 JwtAuthFilter에서 처리함
        // 400 Bad Request는 GlobalExceptionHandler 에서 처리
    }


}