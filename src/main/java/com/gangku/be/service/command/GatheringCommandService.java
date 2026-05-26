package com.gangku.be.service.command;

import com.gangku.be.constant.participation.ParticipationRole;
import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import com.gangku.be.dto.gathering.request.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.request.GatheringUpdateRequestDto;
import com.gangku.be.dto.gathering.response.GatheringResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.CategoryErrorCode;
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.util.object.FileUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GatheringCommandService {

    private final GatheringRepository gatheringRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;
    private final FileUrlResolver fileUrlResolver;

    @Transactional
    public GatheringResponseDto saveGathering(GatheringCreateRequestDto request, Long hostId) {

        User host = findUserById(hostId);
        Category category = findCategoryByName(request.getCategory());

        Gathering gathering =
                Gathering.create(
                        host,
                        category,
                        request.getTitle(),
                        request.getDescription(),
                        request.getGatheringImageObjectKey(),
                        request.getCapacity(),
                        request.getDate(),
                        request.getLocation(),
                        request.getOpenChatUrl());

        Gathering savedGathering = gatheringRepository.save(gathering);

        Participation participation =
                Participation.create(host, savedGathering, ParticipationRole.HOST);
        participationRepository.save(participation);

        return GatheringResponseDto.from(
                savedGathering,
                fileUrlResolver.toPublicUrl(gathering.getGatheringImageObjectKey()));
    }

    @Transactional
    public GatheringResponseDto updateGathering(
            Long gatheringId, Long userId, GatheringUpdateRequestDto request) {

        Gathering gathering = findGatheringById(gatheringId);
        validateGatheringHost(userId, gathering);
        updateRequestBody(request, gathering);
        Gathering updatedGathering = gatheringRepository.save(gathering);

        return GatheringResponseDto.from(
                updatedGathering,
                fileUrlResolver.toPublicUrl(updatedGathering.getGatheringImageObjectKey()));
    }

    private User findUserById(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    private Category findCategoryByName(String categoryName) {
        if (categoryName == null) return null;
        return categoryRepository
                .findByName(categoryName)
                .orElseThrow(() -> new CustomException(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }

    private Gathering findGatheringById(Long gatheringId) {
        return gatheringRepository
                .findById(gatheringId)
                .orElseThrow(() -> new CustomException(GatheringErrorCode.GATHERING_NOT_FOUND));
    }

    private void validateGatheringHost(Long userId, Gathering gathering) {
        if (!gathering.getHost().getId().equals(userId)) {
            throw new CustomException(GatheringErrorCode.NO_PERMISSION_TO_MANIPULATE_GATHERING);
        }
    }

    private void updateRequestBody(GatheringUpdateRequestDto request, Gathering gathering) {
        if (request.getTitle() != null) gathering.setTitle(request.getTitle());
        if (request.getGatheringImageObjectKey() != null)
            gathering.setGatheringImageObjectKey(request.getGatheringImageObjectKey());
        if (request.getCategory() != null && !request.getCategory().isBlank())
            gathering.setCategory(findCategoryByName(request.getCategory()));
        if (request.getCapacity() != null) gathering.setCapacity(request.getCapacity());
        if (request.getDate() != null) gathering.setDate(request.getDate());
        if (request.getLocation() != null) gathering.setLocation(request.getLocation());
        if (request.getOpenChatUrl() != null) gathering.setOpenChatUrl(request.getOpenChatUrl());
        if (request.getDescription() != null) gathering.setDescription(request.getDescription());
    }
}
