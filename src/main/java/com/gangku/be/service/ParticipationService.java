package com.gangku.be.service;

import com.gangku.be.constant.gathering.GatheringStatus;
import com.gangku.be.constant.participation.ParticipationRole;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import com.gangku.be.dto.participation.ParticipantsPreviewResponseDto;
import com.gangku.be.dto.participation.ParticipationResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.exception.constant.ParticipationErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.model.participation.ParticipantsPreview;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.util.object.FileUrlResolver;
import lombok.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final GatheringRepository gatheringRepository;
    private final UserRepository userRepository;
    private final FileUrlResolver fileUrlResolver;

    @Transactional
    public ParticipationResponseDto joinParticipation(Long gatheringId, Long userId) {

        Gathering gathering = findGatheringById(gatheringId);
        User user = findUserById(userId);

        validateConflict(user, gathering);

        Participation participation = Participation.create(user, gathering, ParticipationRole.GUEST);

        gathering.increaseParticipantCount();

        participationRepository.save(participation);

        return ParticipationResponseDto.from(participation, gathering, user);
    }

    @Transactional
    public void cancelParticipation(Long gatheringId, Long userId) {

        Gathering gathering = findGatheringById(gatheringId);
        User user = findUserById(userId);

        Participation participation = verifyUserInParticipation(user, gathering);

        validateGatheringStatus(gathering);

        // 양방향 동기화
        participation.unlink();

        gathering.decreaseParticipantCount();

        // DB에서 참여 정보 삭제
        participationRepository.delete(participation);
    }

    @Transactional(readOnly = true)
    public ParticipantsPreviewResponseDto getParticipants(
            Long gatheringId, int page, int size, String sortParam) {

        findGatheringById(gatheringId);

        String[] parts = sortParam.split(",");
        String dirStr = (parts.length > 1) ? parts[1].toLowerCase() : "asc";

        Sort.Direction direction = dirStr.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, "joinedAt").and(Sort.by(direction, "id"));

        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Page<Participation> participationPage =
                participationRepository.findByGatheringId(gatheringId, pageable);

        String sortedByForSpec = "joinedAt," + dirStr + ",id," + dirStr;

        ParticipantsPreview participantsPreview =
                ParticipantsPreview.from(
                        participationPage, sortedByForSpec, this::resolveProfileImageUrl);

        return ParticipantsPreviewResponseDto.from(participantsPreview);
    }

    private String resolveProfileImageUrl(User user) {
        String key = user.getProfileImageObjectKey();
        if (key == null || key.isBlank()) {
            return null;
        }
        return fileUrlResolver.toPublicUrl(key);
    }

    private Participation verifyUserInParticipation(User user, Gathering gathering) {
        Participation participation =
                participationRepository
                        .findByUserAndGathering(user, gathering)
                        .orElseThrow(
                                () -> new CustomException(ParticipationErrorCode.ALREADY_LEFT));

        if (gathering.getHost().getId().equals(user.getId())) {
            throw new CustomException(ParticipationErrorCode.HOST_CANNOT_LEAVE);
        }
        return participation;
    }

    private void validateGatheringStatus(Gathering gathering) {
        if (gathering.getStatus().equals(GatheringStatus.FINISHED)) {
            throw new CustomException(ParticipationErrorCode.GATHERING_IS_FINISHED);
        }
    }

    private void validateConflict(User user, Gathering gathering) {
        if (participationRepository.existsByUserAndGathering(user, gathering)) {
            throw new CustomException(ParticipationErrorCode.ALREADY_JOINED);
        }

        if (gathering.getParticipantCount() >= gathering.getCapacity()) {
            throw new CustomException(ParticipationErrorCode.CAPACITY_FULL);
        }

        validateGatheringStatus(gathering);
    }

    private User findUserById(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    private Gathering findGatheringById(Long gatheringId) {
        return gatheringRepository
                .findById(gatheringId)
                .orElseThrow(() -> new CustomException(GatheringErrorCode.GATHERING_NOT_FOUND));
    }
}
