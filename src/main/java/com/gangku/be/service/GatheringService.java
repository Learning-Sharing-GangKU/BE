package com.gangku.be.service;

import com.gangku.be.constant.gathering.GatheringSort;
import com.gangku.be.constant.gathering.GatheringStatus;
import com.gangku.be.constant.participation.ParticipationRole;
import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import com.gangku.be.dto.ai.AiRecommendRequestDto;
import com.gangku.be.dto.gathering.request.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.request.GatheringIntroRequestDto;
import com.gangku.be.dto.gathering.request.GatheringUpdateRequestDto;
import com.gangku.be.dto.gathering.response.*;
import com.gangku.be.dto.gathering.response.GatheringDetailResponseDto;
import com.gangku.be.dto.gathering.response.GatheringIntroResponseDto;
import com.gangku.be.dto.gathering.response.GatheringResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.CategoryErrorCode;
import com.gangku.be.exception.constant.CommonErrorCode;
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.external.ai.AiIntroClient;
import com.gangku.be.external.ai.AiRecommendationWebClient;
import com.gangku.be.model.gathering.GatheringList;
import com.gangku.be.model.participation.ParticipantsPreview;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.util.object.FileUrlResolver;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class GatheringService {

    private final GatheringRepository gatheringRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;

    private final WebClient webClient;
    private final FileUrlResolver fileUrlResolver;
    private final AiRecommendationWebClient aiRecommendationWebClient;
    private final AiIntroClient aiIntroClient;

    @Value("${ai.server.base-url}")
    private String aiServerBaseUrl;

    // 모임 생성 메서드
    @Transactional
    public GatheringResponseDto createGathering(
            GatheringCreateRequestDto gatheringCreateRequestDto, Long hostId) {
        /*
        SOL A
        총 두 번 보내야됨 -> title 모임 제목 한 번 / description 모임 소개문 한 번.
        (POST)http://127.0.0.1:8000/api/ai/v1/text/filter으로 보내줘여됨(url 주소 확인 바람.)
        request = {
                scenario = "title"
                text = gatheringUpdateRequestDto.getTitle()
        }

        request = {
                scenario = "description"
                text = gatheringCreateRequestDto.getDescription()
        }
        -> 근데 총 두 번 보내야돼서 좀 좃같을 수도 있음

        SOL B
        gatheringUpdateRequestDto.getTitle() + gatheringCreateRequestDto.getDescription()
        문자열 concat
        해서
        request = {
                scenario = "description"
                text = gatheringUpdateRequestDto.getTitle() + gatheringCreateRequestDto.getDescription()
        }
        로 보내도 됨 -> 근데 title에서 지랄난건지 Description에서 지랄난건지 알기 힘듦
         */

        User host = findUserById(hostId);

        Category category = findCategoryByName(gatheringCreateRequestDto.getCategory());

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
        Participation participation = Participation.create(host, savedGathering, ParticipationRole.HOST);
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
        /*
        SOL A
        총 두 번 보내야됨 -> title 모임 제목 한 번 / description 모임 소개문 한 번.
        (POST)http://127.0.0.1:8000/api/ai/v1/text/filter으로 보내줘여됨(url 주소 확인 바람.)
        request = {
                scenario = "title"
                text = gatheringUpdateRequestDto.getTitle()
        }

        request = {
                scenario = "description"
                text = gatheringCreateRequestDto.getDescription()
        }
        -> 근데 총 두 번 보내야돼서 좀 좃같을 수도 있음

        SOL B
        gatheringUpdateRequestDto.getTitle() + gatheringCreateRequestDto.getDescription()
        문자열 concat
        해서
        (POST)http://127.0.0.1:8000/api/ai/v1/text/filter으로 보내줘여됨(url 주소 확인 바람.)
        request = {
                scenario = "description"
                text = gatheringUpdateRequestDto.getTitle() + gatheringCreateRequestDto.getDescription()
        }
        로 보내도 됨 -> 근데 title에서 지랄난건지 Description에서 지랄난건지 알기 힘듦
         */

        Gathering gathering = findGatheringById(gatheringId);

        validateGatheringHost(userId, gathering);

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
            Long gatheringId, int page, int size, String sortParam) {

        Gathering gathering = findGatheringById(gatheringId);

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

        String gatheringImageUrl = null;
        String gatheringKey = gathering.getGatheringImageObjectKey();
        if (gatheringKey != null && !gatheringKey.isBlank()) {
            gatheringImageUrl = fileUrlResolver.toPublicUrl(gatheringKey);
        }

        return GatheringDetailResponseDto.from(gathering, participantsPreview, gatheringImageUrl);
    }

    // 외부 AI 호출만 -> Client로 위임
    @Transactional
    public GatheringIntroResponseDto createGatheringIntro(
            GatheringIntroRequestDto gatheringIntroRequestDto) {
        return aiIntroClient.createIntro(gatheringIntroRequestDto);
    }

    /**
     * 모임 목록 조회 - 홈 화면 및 카테고리 페이지에서 사용 - category, sort, size에 따라 정렬 및 필터링 - sort = latest →
     * createdAt DESC - sort = popular → participantCount DESC
     */
    @Transactional(readOnly = true)
    public GatheringListResponseDto getGatheringList(
            String categoryName, int page, int size, String sort) {

        Category category = verifyCategoryName(categoryName);

        GatheringSort sortType = GatheringSort.from(sort);

        Sort springSort =
                switch (sortType) {
                    case POPULAR ->
                            Sort.by(Sort.Order.desc("participantCount"), Sort.Order.desc("id"));
                    case LATEST -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
                        /*
                        위치가 정확한지는 모르겠지만 일단.
                        (POST)http://127.0.0.1:8000/api/ai/v2/recommendations
                        request 로 com.gangku.be.dto.gathering.request.GatheringRecommendAiRequest 이거 넘겨주면 됨
                        그럼 response 로 List<gatheringId> 가 return
                         */
                };

        Pageable pageable = PageRequest.of(page - 1, size, springSort);

        Page<Gathering> gatheringPage =
                (sortType == GatheringSort.POPULAR)
                        ? gatheringRepository.findPopularGatherings(pageable)
                        : gatheringRepository.findLatestGatherings(pageable);

        String sortedByForSpec =
                (sortType == GatheringSort.POPULAR)
                        ? "participantCount,desc,id,desc"
                        : "createdAt,desc,id,desc";

        GatheringList gatheringList =
                GatheringList.from(gatheringPage, sortedByForSpec, this::resolveGatheringImageUrl);

        return GatheringListResponseDto.from(gatheringList);
    }

    @Transactional(readOnly = true)
    public GatheringListResponseDto getRecommendedGatherings(Long userId, int page, int size) {

        if (userId == null) {
            return getGatheringList(null, page, size, "latest");
        }

        User user = findUserById(userId);

        List<String> preferredCategories =
                user.getPreferredCategories().stream()
                        .map(pc -> pc.getCategory().getName())
                        .toList();

        List<Gathering> candidates =
                gatheringRepository.findTop50ByStatusOrderByCreatedAtDesc(GatheringStatus.RECRUITING);

        AiRecommendRequestDto aiRecommendRequestDto =
                AiRecommendRequestDto.from(user, preferredCategories, candidates);
        List<Long> recommendedIds = aiRecommendationWebClient.recommend(aiRecommendRequestDto);

        if (recommendedIds == null || recommendedIds.isEmpty()) {
            return getGatheringList(null, page, size, "latest");
        }

        Page<Gathering> gatheringPage = buildRecommendedPage(recommendedIds, page, size);

        String sortedByForSpec = "aiRecommended,desc";
        GatheringList gatheringList =
                GatheringList.from(gatheringPage, sortedByForSpec, this::resolveGatheringImageUrl);
        return GatheringListResponseDto.from(gatheringList);
    }

    /** 사용자별 모임 목록 조회 (role=host | guest) - host: 내가 만든 모임 - guest: 내가 참여한 모임 */
    @Transactional(readOnly = true)
    public GatheringListResponseDto getUserGatherings(
            Long userId, String role, int page, int size, String sort) {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Gathering> gatheringPage;
        String sortedByForSpec;

        if ("host".equals(role)) {
            gatheringPage = gatheringRepository.findByHostIdOrderByCreatedAtDesc(user, pageable);
            sortedByForSpec = "createdAt,desc,id,desc";
        } else if ("guest".equals(role)) {
            gatheringPage = participationRepository.findJoinedGatheringsByUserId(userId, pageable);
            sortedByForSpec = "joinedAt,desc,userId,desc";
        } else {
            // role 값이 잘못 들어온 케이스
            throw new CustomException(
                    CommonErrorCode.INVALID_REQUEST_PARAMETER); // 너희 프로젝트 파라미터 에러코드로 교체
        }

        GatheringList gatheringList =
                GatheringList.from(gatheringPage, sortedByForSpec, this::resolveGatheringImageUrl);

        return GatheringListResponseDto.from(gatheringList);
    }

    /**
     * recommendedIds 순서를 그대로 유지하면서, DB에서 Gathering을 조회해 Page로 만든다. - 삭제된 gatheringId가 섞여 있어도 null은
     * 제거 - page/size에 맞춰 slice 후 PageImpl 생성
     */
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

    private User findUserById(Long hostId) {
        User host =
                userRepository
                        .findById(hostId)
                        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        return host;
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
        if (gatheringUpdateRequestDto.getCategory() != null)
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

    private Category verifyCategoryName(String categoryName) {
        Category category = null;
        if (categoryName != null && !categoryName.isBlank()) {
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
}
