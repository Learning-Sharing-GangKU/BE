package com.gangku.be.service;

import com.gangku.be.constant.gathering.GatheringSort;
import com.gangku.be.constant.gathering.GatheringStatus;
import com.gangku.be.constant.participation.ParticipationRole;
import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import com.gangku.be.dto.ai.request.IntroCreateRequestDto;
import com.gangku.be.dto.ai.request.RecommendationRequestDto;
import com.gangku.be.dto.ai.request.TextFilterRequestDto;
import com.gangku.be.dto.ai.response.IntroCreateResponseDto;
import com.gangku.be.dto.ai.response.TextFilterResponseDto;
import com.gangku.be.dto.gathering.request.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.request.GatheringUpdateRequestDto;
import com.gangku.be.dto.gathering.response.*;
import com.gangku.be.dto.gathering.response.GatheringDetailResponseDto;
import com.gangku.be.dto.gathering.response.GatheringResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.CategoryErrorCode;
import com.gangku.be.exception.constant.CommonErrorCode;
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.external.ai.AiApiClient;
import com.gangku.be.model.gathering.GatheringList;
import com.gangku.be.model.participation.ParticipantsPreview;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.util.ai.AiTextFilterMapper;
import com.gangku.be.util.object.FileUrlResolver;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GatheringService {

    private final GatheringRepository gatheringRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;

    private final FileUrlResolver fileUrlResolver;
    private final AiApiClient aiApiClient;
    private final AiTextFilterMapper aiTextFilterMapper;

    // 모임 생성 메서드
    @Transactional
    public GatheringResponseDto createGathering(
            GatheringCreateRequestDto gatheringCreateRequestDto, Long hostId) {

        User host = findUserById(hostId);

        Category category = findCategoryByName(gatheringCreateRequestDto.getCategory());

//        validateGatheringContentFromGatheringCreate(gatheringCreateRequestDto);

        // 엔티티 생성
        Gathering gathering =
                Gathering.create(
                        host,
                        category,
                        gatheringCreateRequestDto.getTitle(),
                        gatheringCreateRequestDto.getDescription(),
                        gatheringCreateRequestDto.getGatheringImageObjectKey(),
                        gatheringCreateRequestDto.getCapacity(),
                        gatheringCreateRequestDto.getDate(),
                        gatheringCreateRequestDto.getLocation(),
                        gatheringCreateRequestDto.getOpenChatUrl());
        Gathering savedGathering = gatheringRepository.save(gathering);

        // 호스트도 참여자로 추가
        Participation participation =
                Participation.create(host, savedGathering, ParticipationRole.HOST);
        participationRepository.save(participation);

        // 4. 응답 DTO 생성
        return GatheringResponseDto.from(
                savedGathering,
                fileUrlResolver.toPublicUrl(gathering.getGatheringImageObjectKey()));
    }

    // 모임 수정 메서드
    @Transactional
    public GatheringResponseDto updateGathering(
            Long gatheringId, Long userId, GatheringUpdateRequestDto gatheringUpdateRequestDto) {

        Gathering gathering = findGatheringById(gatheringId);

        validateGatheringHost(userId, gathering);

        validateGatheringContentFromGatheringUpdate(gatheringUpdateRequestDto);

        updateRequestBody(gatheringUpdateRequestDto, gathering);

        Gathering updatedGathering = gatheringRepository.save(gathering);

        return GatheringResponseDto.from(
                updatedGathering,
                fileUrlResolver.toPublicUrl(updatedGathering.getGatheringImageObjectKey()));
    }

    // 모임 삭제 메서드
    @Transactional
    public void deleteGathering(Long gatheringId, Long userId) {

        Gathering gathering = findGatheringById(gatheringId);

        validateGatheringHost(userId, gathering);

        gatheringRepository.delete(gathering);
    }

    @Transactional
    public void finishGathering(Long gatheringId, Long userId) {

        Gathering gathering = findGatheringById(gatheringId);

        validateGatheringHost(userId, gathering);

        gathering.changeStatusAsFinished();

        gatheringRepository.save(gathering);
    }

    @Transactional(readOnly = true)
    public GatheringDetailResponseDto getGatheringDetail(
            Long gatheringId, int page, int size, Long userId) {

        Gathering gathering = findGatheringById(gatheringId);
        User user = findUserById(userId);

        boolean joined = participationRepository.existsByUserAndGathering(user, gathering);

        Sort sort =
                Sort.by(Sort.Direction.DESC, "joinedAt").and(Sort.by(Sort.Direction.DESC, "id"));

        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Page<Participation> participationPage =
                participationRepository.findByGatheringId(gatheringId, pageable);

        String sortedByForSpec = "joinedAt,desc";
        ParticipantsPreview participantsPreview =
                ParticipantsPreview.from(
                        participationPage, sortedByForSpec, this::resolveProfileImageUrl);

        String gatheringImageUrl = null;
        String gatheringKey = gathering.getGatheringImageObjectKey();
        if (gatheringKey != null && !gatheringKey.isBlank()) {
            gatheringImageUrl = fileUrlResolver.toPublicUrl(gatheringKey);
        }

        return GatheringDetailResponseDto.from(
                gathering, participantsPreview, gatheringImageUrl, joined);
    }

    // 외부 AI 호출만 -> Client로 위임
    @Transactional
    public IntroCreateResponseDto createGatheringIntro(
            IntroCreateRequestDto introCreateRequestDto) {
        return aiApiClient.createIntro(introCreateRequestDto);
    }

    @Transactional(readOnly = true)
    public GatheringListResponseDto getGatheringList(
            Long userId, String categoryName, int page, int size, String sort) {

        Category category = findCategoryByName(categoryName);
        GatheringSort sortType = GatheringSort.from(sort);

        Page<Gathering> gatheringPage =
                switch (sortType) {
                    case LATEST, POPULAR -> getNormalGatheringPage(category, sortType, page, size);
                    case RECOMMEND -> getRecommendedGatheringPage(userId, category, page, size);
                };

        String sortedByForSpec = getSortedByForSpec(sortType);

        GatheringList gatheringList =
                GatheringList.from(gatheringPage, sortedByForSpec, this::resolveGatheringImageUrl);

        return GatheringListResponseDto.from(gatheringList);
    }

    @Transactional(readOnly = true)
    public GatheringListResponseDto getUserGatheringList(
            Long userId, String role, int page, int size) {

        User user = findUserById(userId);

        Page<Gathering> gatheringPage;
        String sortedByForSpec;

        if ("host".equalsIgnoreCase(role)) {
            Pageable pageable =
                    PageRequest.of(
                            page - 1,
                            size,
                            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
            gatheringPage = gatheringRepository.findByHostId(user, pageable);
            sortedByForSpec = "createdAt,desc";
        } else if ("guest".equalsIgnoreCase(role)) {
            Pageable pageable = PageRequest.of(page - 1, size);
            gatheringPage = participationRepository.findJoinedGatheringsByUserId(userId, pageable);
            sortedByForSpec = "joinedAt,desc";
        } else {
            throw new CustomException(CommonErrorCode.INVALID_REQUEST_PARAMETER);
        }

        GatheringList gatheringList =
                GatheringList.from(gatheringPage, sortedByForSpec, this::resolveGatheringImageUrl);

        return GatheringListResponseDto.from(gatheringList);
    }

    private Page<Gathering> getNormalGatheringPage(
            Category category, GatheringSort sortType, int page, int size) {

        Sort springSort =
                switch (sortType) {
                    case POPULAR ->
                            Sort.by(Sort.Order.desc("participantCount"), Sort.Order.desc("id"));
                    case LATEST -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
                    default -> throw new CustomException(CommonErrorCode.INVALID_REQUEST_PARAMETER);
                };

        Pageable pageable = PageRequest.of(page - 1, size, springSort);
        return getGatheringPage(category, sortType, pageable);
    }

    private Page<Gathering> getRecommendedGatheringPage(
            Long userId, Category category, int page, int size) {

        if (userId == null) {
            return getNormalGatheringPage(category, GatheringSort.LATEST, page, size);
        }

        User user = findUserById(userId);

        List<String> preferredCategories =
                user.getPreferredCategories().stream()
                        .map(pc -> pc.getCategory().getName())
                        .toList();

        List<Gathering> candidates = getRecommendationCandidates(category);

        if (candidates.isEmpty()) {
            return getNormalGatheringPage(category, GatheringSort.LATEST, page, size);
        }

        RecommendationRequestDto recommendationRequestDto =
                RecommendationRequestDto.from(user, preferredCategories, candidates);

        List<Long> recommendedIds =
                aiApiClient.recommend(recommendationRequestDto).getGatheringsId();

        if (recommendedIds == null || recommendedIds.isEmpty()) {
            return getNormalGatheringPage(category, GatheringSort.LATEST, page, size);
        }

        return buildRecommendedPage(recommendedIds, page, size);
    }

    private List<Gathering> getRecommendationCandidates(Category category) {
        if (category != null) {
            return gatheringRepository.findTop50ByCategoryAndStatusOrderByCreatedAtDesc(
                    category, GatheringStatus.RECRUITING);
        }

        return gatheringRepository.findTop50ByStatusOrderByCreatedAtDesc(
                GatheringStatus.RECRUITING);
    }

    private String getSortedByForSpec(GatheringSort sortType) {
        return switch (sortType) {
            case POPULAR -> "participantCount,desc,id,desc";
            case LATEST -> "createdAt,desc,id,desc";
            case RECOMMEND -> "recommended,desc";
        };
    }

    private Page<Gathering> buildRecommendedPage(List<Long> recommendedIds, int page, int size) {

        // 1) DB 조회
        List<Gathering> found = gatheringRepository.findByIdIn(recommendedIds);

        // 2) id -> entity map
        Map<Long, Gathering> byId =
                found.stream().collect(Collectors.toMap(Gathering::getId, Function.identity()));

        // 3) AI가 준 순서대로 재정렬 (삭제된 건 제외)
        List<Gathering> ordered =
                recommendedIds.stream().map(byId::get).filter(Objects::nonNull).toList();

        // 4) 페이지 슬라이스
        int totalElements = ordered.size();
        int fromIndex = Math.min((page - 1) * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<Gathering> content = ordered.subList(fromIndex, toIndex);

        // 5) Page로 래핑
        Pageable pageable = PageRequest.of(page - 1, size);
        return new PageImpl<>(content, pageable, totalElements);
    }

    private String resolveGatheringImageUrl(Gathering g) {
        return resolveImageUrl(g.getGatheringImageObjectKey());
    }

    private String resolveProfileImageUrl(User user) {
        return resolveImageUrl(user.getProfileImageObjectKey());
    }

    private String resolveImageUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return fileUrlResolver.toPublicUrl(key);
    }

    private User findUserById(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    private Category findCategoryByName(String categoryName) {
        Category category = null;
        if (categoryName != null) {
            category =
                    categoryRepository
                            .findByName(categoryName)
                            .orElseThrow(
                                    () ->
                                            new CustomException(
                                                    CategoryErrorCode.CATEGORY_NOT_FOUND));
        }
        return category;
    }

    public Gathering findGatheringById(Long gatheringId) {
        return gatheringRepository
                .findById(gatheringId)
                .orElseThrow(() -> new CustomException(GatheringErrorCode.GATHERING_NOT_FOUND));
    }

    private void updateRequestBody(
            GatheringUpdateRequestDto gatheringUpdateRequestDto, Gathering gathering) {
        if (gatheringUpdateRequestDto.getTitle() != null)
            gathering.setTitle(gatheringUpdateRequestDto.getTitle());
        if (gatheringUpdateRequestDto.getGatheringImageObjectKey() != null)
            gathering.setGatheringImageObjectKey(
                    gatheringUpdateRequestDto.getGatheringImageObjectKey());
        if (gatheringUpdateRequestDto.getCategory() != null && !gatheringUpdateRequestDto.getCategory().isBlank())
            gathering.setCategory(findCategoryByName(gatheringUpdateRequestDto.getCategory()));
        if (gatheringUpdateRequestDto.getCapacity() != null)
            gathering.setCapacity(gatheringUpdateRequestDto.getCapacity());
        if (gatheringUpdateRequestDto.getDate() != null)
            gathering.setDate(gatheringUpdateRequestDto.getDate());
        if (gatheringUpdateRequestDto.getLocation() != null)
            gathering.setLocation(gatheringUpdateRequestDto.getLocation());
        if (gatheringUpdateRequestDto.getOpenChatUrl() != null)
            gathering.setOpenChatUrl(gatheringUpdateRequestDto.getOpenChatUrl());
        if (gatheringUpdateRequestDto.getDescription() != null)
            gathering.setDescription(gatheringUpdateRequestDto.getDescription());
    }

    private void validateGatheringHost(Long userId, Gathering gathering) {
        if (!gathering.getHost().getId().equals(userId)) {
            throw new CustomException(GatheringErrorCode.NO_PERMISSION_TO_MANIPULATE_GATHERING);
        }
    }

    private void validateGatheringContentFromGatheringCreate(
            GatheringCreateRequestDto gatheringCreateRequestDto) {
        TextFilterRequestDto textFilterRequestDto =
                aiTextFilterMapper.fromGatheringCreate(gatheringCreateRequestDto);
        TextFilterResponseDto textFilterResponseDto = aiApiClient.filterText(textFilterRequestDto);

        if (!textFilterResponseDto.isAllowed()) {
            throw new CustomException(GatheringErrorCode.INVALID_GATHERING_CONTENT);
        }
    }

    private void validateGatheringContentFromGatheringUpdate(
            GatheringUpdateRequestDto gatheringUpdateRequestDto) {
        boolean hasTitle =
                gatheringUpdateRequestDto.getTitle() != null
                        && !gatheringUpdateRequestDto.getTitle().isBlank();
        boolean hasDescription =
                gatheringUpdateRequestDto.getDescription() != null
                        && !gatheringUpdateRequestDto.getDescription().isBlank();

        if (!hasTitle && !hasDescription) {
            return;
        }

        TextFilterRequestDto textFilterRequestDto =
                aiTextFilterMapper.fromGatheringUpdate(gatheringUpdateRequestDto);

        TextFilterResponseDto textFilterResponseDto = aiApiClient.filterText(textFilterRequestDto);

        if (!textFilterResponseDto.isAllowed()) {
            throw new CustomException(GatheringErrorCode.INVALID_GATHERING_CONTENT);
        }
    }

    private Page<Gathering> getGatheringPage(
            Category category, GatheringSort sortType, Pageable pageable) {
        Page<Gathering> gatheringPage;
        if (category != null) {
            gatheringPage =
                    (sortType == GatheringSort.POPULAR)
                            ? gatheringRepository.findPopularGatheringsByCategory(
                                    category, pageable)
                            : gatheringRepository.findLatestGatheringsByCategory(
                                    category, pageable);
        } else {
            gatheringPage =
                    (sortType == GatheringSort.POPULAR)
                            ? gatheringRepository.findPopularGatherings(pageable)
                            : gatheringRepository.findLatestGatherings(pageable);
        }
        return gatheringPage;
    }
}
