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
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.support.GatheringLookup;
import com.gangku.be.support.UserLookup;
import com.gangku.be.util.cache.HomeCache;
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
    private final UserLookup userLookup;
    private final GatheringLookup gatheringLookup;
    private final FileUrlResolver fileUrlResolver;
    private final HomeCache homeCache;

    @Transactional
    public GatheringResponseDto saveGathering(GatheringCreateRequestDto request, Long hostId) {

        User host = userLookup.findById(hostId);
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

        homeCache.invalidateHome();

        return GatheringResponseDto.from(
                savedGathering,
                fileUrlResolver.toPublicUrl(gathering.getGatheringImageObjectKey()));
    }

    @Transactional
    public GatheringResponseDto updateGathering(
            Long gatheringId, Long userId, GatheringUpdateRequestDto request) {

        Gathering gathering = gatheringLookup.findById(gatheringId);
        validateGatheringHost(userId, gathering);

        Category newCategory =
                (request.getCategory() != null && !request.getCategory().isBlank())
                        ? findCategoryByName(request.getCategory())
                        : null;
        gathering.updateDetails(
                request.getTitle(),
                request.getDescription(),
                request.getGatheringImageObjectKey(),
                request.getCapacity(),
                request.getDate(),
                request.getLocation(),
                request.getOpenChatUrl(),
                newCategory);

        Gathering updatedGathering = gatheringRepository.save(gathering);

        homeCache.invalidateHome();

        return GatheringResponseDto.from(
                updatedGathering,
                fileUrlResolver.toPublicUrl(updatedGathering.getGatheringImageObjectKey()));
    }

    private Category findCategoryByName(String categoryName) {
        if (categoryName == null) return null;
        return categoryRepository
                .findByName(categoryName)
                .orElseThrow(() -> new CustomException(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }

    private void validateGatheringHost(Long userId, Gathering gathering) {
        if (!gathering.getHost().getId().equals(userId)) {
            throw new CustomException(GatheringErrorCode.NO_PERMISSION_TO_MANIPULATE_GATHERING);
        }
    }
}
