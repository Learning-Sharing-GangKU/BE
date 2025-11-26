package com.gangku.be.service;

import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import com.gangku.be.dto.participation.ParticipationResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.exception.constant.ParticipationErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
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
    public ParticipationResponseDto joinParticipation(Long gatheringId, Long userId) {

        // 401 Unauthorized 는 JwtAuthFilter에서 처리

        Gathering gathering = findGatheringById(gatheringId);
        User user = findUserById(userId);

        validateConflict(user, gathering);

        Participation participation = Participation.create(user, gathering);
        gathering.addParticipation(participation);
        participationRepository.save(participation);
        // 이거 테스트 해보고 지울 수 있으면 지우자
//        gatheringRepository.save(gathering);

        return ParticipationResponseDto.from(participation, gathering, user);
    }

    // 침여 취소 메서드
    @Transactional
    public void cancelParticipation(Long gatheringId, Long userId) {

        Gathering gathering = findGatheringById(gatheringId);
        User user = findUserById(userId);

        // 참여자 정보 확인
        Participation participation = verifyUserInParticipation(user, gathering);

        // 모임 객체에서 Participation 제거 (양방향일 경우)
        gathering.removeParticipation(participation);

        // DB에서 참여 정보 삭제
        participationRepository.delete(participation);
    }

    private Participation verifyUserInParticipation(User user, Gathering gathering) {
        Participation participation = participationRepository
                .findByUserAndGathering(user, gathering)
                .orElseThrow(() -> new CustomException(ParticipationErrorCode.ALREADY_LEFT));

        if (gathering.getHost().getId().equals(user.getId())) {
            throw new CustomException(ParticipationErrorCode.HOST_CANNOT_LEAVE);
        }
        return participation;
    }

    private void validateConflict(User user, Gathering gathering) {
        if (participationRepository.existsByUserAndGathering(user, gathering)) {
            throw new CustomException(ParticipationErrorCode.ALREADY_JOINED);
        }

        if (gathering.getParticipantCount() >= gathering.getCapacity()) {
            throw new CustomException(ParticipationErrorCode.CAPACITY_FULL);
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    private Gathering findGatheringById(Long gatheringId) {
        return gatheringRepository.findById(gatheringId)
                .orElseThrow(() -> new CustomException(GatheringErrorCode.GATHERING_NOT_FOUND));
    }
}