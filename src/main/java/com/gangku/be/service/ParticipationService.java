package com.gangku.be.service;

import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.Participation.Role;
import com.gangku.be.domain.User;
import com.gangku.be.dto.participation.ParticipantsPreviewResponseDto;
import com.gangku.be.dto.participation.ParticipationResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.exception.constant.ParticipationErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.model.ParticipantsPreview;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.util.object.FileUrlResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final GatheringRepository gatheringRepository;
    private final UserRepository userRepository;
    private final FileUrlResolver fileUrlResolver;

    @Transactional
    public ParticipationResponseDto joinParticipation(Long gatheringId, Long userId) {

        // 401 Unauthorized 는 JwtAuthFilter에서 처리

        Gathering gathering = findGatheringById(gatheringId);
        User user = findUserById(userId);

        validateConflict(user, gathering);

        Participation participation = Participation.create(user, gathering, Role.GUEST);
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

    @Transactional(readOnly = true)
    public ParticipantsPreviewResponseDto getParticipants(Long gatheringId, int page, int size, String sortParam) {

        findGatheringById(gatheringId); // 404 찾기 위해 추가

        String dirStr = validateParamsFormatAndParseSortParam(page, size, sortParam);

        Sort.Direction direction = dirStr.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, "joinedAt").and(Sort.by(direction, "id"));

        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Page<Participation> participationPage =
                participationRepository.findByGatheringId(gatheringId, pageable);

        String sortedBy = "joinedAt," + dirStr + ",id," + dirStr;

        ParticipantsPreview participantsPreview = ParticipantsPreview.from(
                participationPage,
                page,
                size,
                sortedBy,
                user -> {
                    String key = user.getProfileImageObjectKey();
                    if (key == null || key.isBlank()) {
                        return null;
                    }
                    return fileUrlResolver.toPublicUrl(key);
                }
        );

        return ParticipantsPreviewResponseDto.from(participantsPreview);
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

    private String validateParamsFormatAndParseSortParam(int page, int size, String sortParam) {
        if  (page < 1 || size < 1 || size > 10) {
            throw new CustomException(ParticipationErrorCode.INVALID_PARAMETER_FORMAT);
        }

        String[] parts = sortParam.split(",");
        String property = (parts.length > 0 && !parts[0].isBlank()) ? parts[0] : "joinedAt";
        String dirStr = (parts.length > 1) ? parts[1].toLowerCase() : "asc";

        if (!property.equals("joinedAt")) {
            throw new CustomException(ParticipationErrorCode.INVALID_PARAMETER_FORMAT);
        }

        return dirStr;
    }
}