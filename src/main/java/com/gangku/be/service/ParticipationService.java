package com.gangku.be.service;

import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import com.gangku.be.dto.common.PageMetaDto;
import com.gangku.be.dto.gathering.response.ParticipantPreviewDto;
import com.gangku.be.dto.gathering.response.ParticipantsPreviewDto;
import com.gangku.be.dto.participation.ParticipationResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.ErrorCode;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final GatheringRepository gatheringRepository;
    private final UserRepository userRepository;


    @Transactional
    public ParticipationResponseDto join(Long gatheringId, User user) {
        System.out.println("[ParticipationService] 참여 요청: gatheringId=" + gatheringId + ", userId=" + user.getId());

        // 401 Unauthorized 는 JwtAuthFilter에서 처리

        Gathering gathering = gatheringRepository.findById(gatheringId)
                .orElseThrow(() -> new CustomException(ErrorCode.GATHERING_NOT_FOUND));

        userRepository.findById(user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 중복 참여 방지
        if (participationRepository.existsByUserAndGathering(user, gathering)) {
            throw new CustomException(ErrorCode.ALREADY_JOINED);
        }

        // 인원 제한 검사
        if (gathering.getParticipantCount() >= gathering.getCapacity()) {
            throw new CustomException(ErrorCode.CAPACITY_FULL);
        }

        Participation participation = new Participation();
        participation.setUser(user);
        participation.setGathering(gathering);
        participation.setStatus(Participation.Status.APPROVED);
        participation.setRole(Participation.ParticipationRole.GUEST);
        gathering.addParticipation(participation);
        participationRepository.save(participation);
        // 참여자 수 증가
        gathering.setParticipantCount(gathering.getParticipantCount() + 1);
        gatheringRepository.save(gathering); // 업데이트 반영
        return new ParticipationResponseDto(
                "gath_" + gathering.getId(),
                "part_" + participation.getId(),
                "usr_" + user.getId(),
                "guest", // TODO: 역할 로직 필요시 수정
                gathering.getParticipantCount(),
                gathering.getCapacity(),
                participation.getJoinedAt()
        );
    }

    // 침여 취소 메서드
    @Transactional
    public void cancelParticipation(Long gatheringId, Long userId) {

        // 모임 존재 확인
        Gathering gathering = gatheringRepository.findById(gatheringId)
                .orElseThrow(() -> new CustomException(ErrorCode.GATHERING_NOT_FOUND));

        // 유저 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 참여자 정보 확인
        Participation participation = participationRepository
                .findByUserAndGathering(user, gathering)
                .orElseThrow(() -> new CustomException(ErrorCode.ALREADY_LEFT));

        // 모임 객체에서 Participation 제거 (양방향일 경우)
        gathering.removeParticipation(participation);

        // DB에서 참여 정보 삭제
        participationRepository.delete(participation);
        // 참여자 수 감소
        gathering.setParticipantCount(gathering.getParticipantCount() -1);
    }

    /**
     * [참여자 목록 조회 서비스 로직]
     * - 특정 모임 ID의 참여자 목록을 페이지 단위로 반환
     * - sort 파라미터는 joinedAt,asc 또는 joinedAt,desc 허용
     */
    @Transactional(readOnly = true)
    public ParticipantsPreviewDto getParticipants(Long gatheringId, int page, int size, String sort) {
        if (gatheringId == null || gatheringId <= 0) {
            throw new CustomException(ErrorCode.INVALID_GATHERING_ID);
        }

        if (size <= 0 || size > 10) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER_FORMAT);
        }

        // 정렬 조건 파싱
        Sort.Direction direction = sort.endsWith("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, "joinedAt", "id"));

        Gathering gathering = gatheringRepository.findById(gatheringId)
                .orElseThrow(() -> new CustomException(ErrorCode.GATHERING_NOT_FOUND));

        Page<Participation> participantPage =
                participationRepository.findByGathering(gathering, pageable);

        // 참여자 DTO 변환
        List<ParticipantPreviewDto> participants = participantPage.stream()
                .map(ParticipantPreviewDto::from)
                .collect(Collectors.toList());

        // meta 정보 구성
        PageMetaDto meta = PageMetaDto.of(
                page,
                size,
                participantPage.getTotalElements(),
                sort
        );

        return new ParticipantsPreviewDto(participants, meta);
    }

}