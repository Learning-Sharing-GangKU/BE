package com.gangku.be.service;

import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import com.gangku.be.dto.participation.ParticipationResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.ErrorCode;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.*;
import org.springframework.stereotype.Service;

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


}