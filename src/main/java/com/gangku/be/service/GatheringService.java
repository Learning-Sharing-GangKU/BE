package com.gangku.be.service;

import com.gangku.be.constant.gathering.GatheringSort;
import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Gathering.Status;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.Participation.Role;
import com.gangku.be.domain.User;
import com.gangku.be.dto.ai.AiRecommendRequestDto;
import com.gangku.be.dto.ai.AiRecommendResponseDto;
import com.gangku.be.dto.gathering.request.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.response.GatheringDetailResponseDto;
import com.gangku.be.dto.gathering.request.GatheringIntroRequestDto;
import com.gangku.be.dto.gathering.response.GatheringIntroResponseDto;
import com.gangku.be.dto.gathering.response.GatheringResponseDto;
import com.gangku.be.dto.gathering.request.GatheringUpdateRequestDto;
import com.gangku.be.dto.gathering.response.*;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.CategoryErrorCode;
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.exception.constant.ParticipationErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@Service
@RequiredArgsConstructor
public class GatheringService {

    private final GatheringRepository gatheringRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;

    private final WebClient webClient;
    private final FileUrlResolver fileUrlResolver;

    @Value("${ai.server.base-url}")
    private String aiServerBaseUrl;

    //모임 생성 메서드
    @Transactional
    public GatheringResponseDto createGathering(
            GatheringCreateRequestDto gatheringCreateRequestDto,
            Long hostId
    ) {
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

        //필드 유효성 검사 예외처리
        validateFields(gatheringCreateRequestDto);

        User host = findUserById(hostId);

        Category category = findCategoryByName(gatheringCreateRequestDto.getCategory());

        // 엔티티 생성
        Gathering gathering = Gathering.create(
                host,
                category,
                gatheringCreateRequestDto.getTitle(),
                gatheringCreateRequestDto.getDescription(),
                gatheringCreateRequestDto.getGatheringImageObjectKey(),
                gatheringCreateRequestDto.getCapacity(),
                gatheringCreateRequestDto.getDate(),
                gatheringCreateRequestDto.getLocation(),
                gatheringCreateRequestDto.getOpenChatUrl()
        );
        Gathering savedGathering = gatheringRepository.save(gathering);

        // 호스트도 참여자로 추가
        Participation participation = Participation.create(host, savedGathering, Role.HOST);
        participationRepository.save(participation);

        // 4. 응답 DTO 생성
        return GatheringResponseDto.from(
                savedGathering,
                fileUrlResolver.toPublicUrl(gathering.getGatheringImageObjectKey())
        );
    }

    // 모임 수정 메서드
    @Transactional
    public GatheringResponseDto updateGathering(
            Long gatheringId,
            Long userId,
            GatheringUpdateRequestDto gatheringUpdateRequestDto
    ) {
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

        // 필드 유효성 검사 예외처리
        validateFields(gatheringUpdateRequestDto);

        Gathering gathering = findGatheringById(gatheringId);

        validateGatheringHost(userId, gathering);

        updateRequestBody(gatheringUpdateRequestDto, gathering);

        Gathering updatedGathering = gatheringRepository.save(gathering);

        return GatheringResponseDto.from(
                updatedGathering,
                fileUrlResolver.toPublicUrl(updatedGathering.getGatheringImageObjectKey())
        );
    }

    // 모임 삭제 메서드
    @Transactional
    public void deleteGathering(Long gatheringId, Long userId) {

        Gathering gathering = findGatheringById(gatheringId);

        validateGatheringHost(userId, gathering);

        gatheringRepository.delete(gathering);
    }

    @Transactional
    public GatheringDetailResponseDto getGatheringDetail(Long gatheringId, int page, int size, String sortParam) {

        Gathering gathering = findGatheringById(gatheringId);

        String dirStr = validateParamsFormatAndParseSortParam(page, size, sortParam);

        Sort.Direction direction = dirStr.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, "joinedAt").and(Sort.by(direction, "id"));

        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Page<Participation> participationPage =
                participationRepository.findByGatheringId(gatheringId, pageable);

        ParticipantsPreview participantsPreview = ParticipantsPreview.from(
                participationPage,
                page,
                size,
                "joinedAt,asc",
                user -> {
                    String key = user.getProfileImageObjectKey();
                    if (key == null || key.isBlank()) {
                        return null;
                    }
                    return fileUrlResolver.toPublicUrl(key);
                }
        );

        String gatheringImageUrl = null;
        String gatheringKey = gathering.getGatheringImageObjectKey();
        if (gatheringKey == null || gatheringKey.isBlank()) {
            gatheringImageUrl = fileUrlResolver.toPublicUrl(gatheringKey);
        }

        return GatheringDetailResponseDto.from(
                gathering,
                participantsPreview,
                gatheringImageUrl
        );
    }

    public GatheringIntroResponseDto createGatheringIntro(GatheringIntroRequestDto gatheringIntroRequestDto) {

        /*
        AI 서버 내부에서 금칙어/비속어 구현해서 따로 뭐 할 건 없음
        "/ai/v1/gatherings/intro"
        일로 타고 들어와도 금칙어 구현 됨
        -> AI에서 response로 뱉는 상태코드에 따라 어떤 상태인지는 노션 보고 다시 한 번 확인해야됨
        2025 12/02 20:47분 기준
         */
        validateKeywords(gatheringIntroRequestDto.getKeywords());

        return webClient.post()
                .uri(aiServerBaseUrl + "/ai/v1/gatherings/intro")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(gatheringIntroRequestDto)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> Mono.error(new CustomException(GatheringErrorCode.AI_SERVICE_UNAVAILABLE))
                )
                .bodyToMono(GatheringIntroResponseDto.class)
                .block();
    }

    /**
     *   모임 목록 조회
     * - 홈 화면 및 카테고리 페이지에서 사용
     * - category, sort, size에 따라 정렬 및 필터링
     *   - sort = latest → createdAt DESC
     *   - sort = popular → participantCount DESC
     */
    @Transactional(readOnly = true)
    public GatheringListResponseDto getGatheringList(String categoryName, int page, int size, String sort) {

        validateParamsFormat(page, size);

        Category category = verifyCategoryName(categoryName);

        GatheringSort sortType = GatheringSort.from(sort);

        Sort springSort = switch (sortType) {
            case POPULAR -> Sort.by(
                    Sort.Order.desc("participantCount"),
                    Sort.Order.desc("id")
            );
            case LATEST -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
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
                        ? gatheringRepository.findPopularGatherings(category, pageable)
                        : gatheringRepository.findLatestGatherings(category, pageable);

        String sortedByForMeta =
                (sortType == GatheringSort.POPULAR)
                        ? "popularScore,desc,id,desc"
                        : "createdAt,desc,id,desc";

        GatheringList gatheringList = GatheringList.from(
                gatheringPage,
                page,
                size,
                sortedByForMeta,
                g -> {
                    String key = g.getGatheringImageObjectKey();
                    if (key == null || key.isBlank()) {
                        return null;
                    }
                    return fileUrlResolver.toPublicUrl(key);
                }
        );

        return GatheringListResponseDto.from(gatheringList);
    }

    @Transactional(readOnly = true)
    public GatheringListResponseDto getRecommendedGatherings(Long userId, int page, int size) {

        if (userId == null) {
            return getGatheringList(null, page, size, "latest");
        }

        validateParamsFormat(page, size);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        List<String> preferredCategories = user.getPreferredCategories().stream()
                .map(pc -> pc.getCategory().getName())
                .toList();

        List<Gathering> candidates = gatheringRepository
                .findTop50ByStatusOrderByCreatedAtDesc(Status.RECRUITING);

        AiRecommendRequestDto aiRecommendRequestDto = AiRecommendRequestDto.from(
                user,
                preferredCategories,
                candidates
        );

        AiRecommendResponseDto aiRecommendResponseDto = webClient.post()
                .uri(aiServerBaseUrl + "/api/ai/v1/recommendations")
                .bodyValue(aiRecommendRequestDto)
                .retrieve()
                .bodyToMono(AiRecommendResponseDto.class)
                .block();

        List<Long> recommendedIds = (aiRecommendResponseDto == null || aiRecommendResponseDto.getItems() == null)
                ? List.of()
                : aiRecommendResponseDto.getItems();

        if (recommendedIds.isEmpty()) {
            return getGatheringList(null, page, size, "latest");
        }

        List<Gathering> foundedGatherings = gatheringRepository.findByIdIn(recommendedIds);

        Map<Long, Gathering> byId = foundedGatherings.stream()
                .collect(Collectors.toMap(Gathering::getId, Function.identity()));

        List<Gathering> ordered = recommendedIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull) // 혹시 삭제된 방이 있을 수도 있으니
                .toList();

        int totalElements = ordered.size();
        int fromIndex = Math.min((page - 1) * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<Gathering> content = ordered.subList(fromIndex, toIndex);

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Gathering> gatheringPage = new PageImpl<>(content, pageable, totalElements);

        String sortedByForMeta = "aiRecommended,desc";

        // 7) 기존 GatheringList / ResponseDto 변환 로직 재사용
        GatheringList gatheringList = GatheringList.from(
                gatheringPage,
                page,
                size,
                sortedByForMeta,
                g -> {
                    String key = g.getGatheringImageObjectKey();
                    if (key == null || key.isBlank()) {
                        return null;
                    }
                    return fileUrlResolver.toPublicUrl(key);
                }
        );

        return GatheringListResponseDto.from(gatheringList);
    }

    /**
     * 사용자별 모임 목록 조회 (role=host | guest)
     * - host: 내가 만든 모임
     * - guest: 내가 참여한 모임
     */
    @Transactional(readOnly = true)
    public GatheringListResponseDto getUserGatherings(Long userId, String role, int page, int size, String sort) {

        if (!role.equals("host") && !role.equals("guest")) {
            throw new CustomException(ParticipationErrorCode.INVALID_ROLE);
        }
        if (size <= 0 || size > 50 || page <= 0) {
            throw new CustomException(GatheringErrorCode.INVALID_PARAMETER_FORMAT);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Gathering> gatheringPage;

        if (role.equals("host")) {
            gatheringPage = gatheringRepository.findByHostIdOrderByCreatedAtDesc(userId, pageable);
        } else {
            gatheringPage = participationRepository.findJoinedGatheringsByUserId(userId, pageable);
        }

        String sortedByForMeta =
                role.equals("host")
                        ? "createdAt,desc,id,desc"
                        : "joinedAt,desc,id,desc";

        GatheringList gatheringList = GatheringList.from(
                gatheringPage,
                page,
                size,
                sortedByForMeta,
                g -> {
                    String key = g.getGatheringImageObjectKey();
                    if (key ==null || key.isBlank()) {
                        return null;
                    }
                    return fileUrlResolver.toPublicUrl(key);
                }
        );

        return GatheringListResponseDto.from(gatheringList);
    }


    private User findUserById(Long hostId) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        return host;
    }

    private Category findCategoryByName(String categoryName) {
        Category category= null;
        if (categoryName != null) {
            category = categoryRepository.findByName(categoryName)
                    .orElseThrow(() -> new CustomException(CategoryErrorCode.CATEGORY_NOT_FOUND));
        }
        return category;
    }

    public Gathering findGatheringById(Long gatheringId) {
        return gatheringRepository.findById(gatheringId)
                .orElseThrow(() -> new CustomException(GatheringErrorCode.GATHERING_NOT_FOUND));
    }

    private void updateRequestBody(GatheringUpdateRequestDto gatheringUpdateRequestDto, Gathering gathering) {
        if (gatheringUpdateRequestDto.getTitle() != null) gathering.setTitle(gatheringUpdateRequestDto.getTitle());
        if (gatheringUpdateRequestDto.getGatheringImageObjectKey() != null) gathering.setGatheringImageObjectKey(gatheringUpdateRequestDto.getGatheringImageObjectKey());
        if (gatheringUpdateRequestDto.getCategory() != null) gathering.setCategory(findCategoryByName(gatheringUpdateRequestDto.getCategory()));
        if (gatheringUpdateRequestDto.getCapacity() != null) gathering.setCapacity(gatheringUpdateRequestDto.getCapacity());
        if (gatheringUpdateRequestDto.getDate() != null) gathering.setDate(gatheringUpdateRequestDto.getDate());
        if (gatheringUpdateRequestDto.getLocation() != null) gathering.setLocation(gatheringUpdateRequestDto.getLocation());
        if (gatheringUpdateRequestDto.getOpenChatUrl() != null) gathering.setOpenChatUrl(gatheringUpdateRequestDto.getOpenChatUrl());
        if (gatheringUpdateRequestDto.getDescription() != null) gathering.setDescription(gatheringUpdateRequestDto.getDescription());
    }

    private void validateGatheringHost(Long userId, Gathering gathering) {
        if (!gathering.getHost().getId().equals(userId)) {
            throw new CustomException(GatheringErrorCode.FORBIDDEN);
        }
    }

    private void validateFields(GatheringCreateRequestDto request) {
        // title: 1~30자
        String title = request.getTitle();
        if (title == null || title.isEmpty() || title.length() > 30) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // imageUrl: URL 형식일 경우만 검사
        String imageObjectKey = request.getGatheringImageObjectKey();
        if (imageObjectKey != null) {
            if (imageObjectKey.isBlank()) {
                throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
            }

            if (!imageObjectKey.matches("^[a-zA-Z0-9/_.-]+$")) {
                throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
            }
        }

        // category: DB에서 조회된 것 중 하나여야 함
        List<String> allowed = categoryRepository.findAll().stream()
                .map(Category::getName)
                .collect(Collectors.toList());
        if (!allowed.contains(request.getCategory())) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // capacity: 1~100
        int capacity = request.getCapacity();
        if (capacity < 1 || capacity > 100) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // date: ISO 8601, 과거 불가
        if (request.getDate() == null || request.getDate().isBefore(LocalDateTime.now())) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // location: 1~30자
        String location = request.getLocation();
        if (location == null || location.length() < 1 || location.length() > 30) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // openChatUrl: https로 시작
        String chatUrl = request.getOpenChatUrl();
        if (chatUrl == null || !chatUrl.startsWith("https://")) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }

        // description: 최대 800자
        String desc = request.getDescription();
        if (desc == null || desc.length() > 800) {
            throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
        }
    }

    // 모임 수정 필드 검증 메서드
    private void validateFields(GatheringUpdateRequestDto request) {

        // 1) title: 제공된 경우에만 검사
        String title = request.getTitle();
        if (title != null) { // <= 여기 중요
            if (title.isEmpty() || title.length() > 30) {
                throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
            }
        }

        // 2) image: null 이면 검사 X, null 아닌 경우만 URL 형식 체크
        String imageObjectKey = request.getGatheringImageObjectKey();
        if (imageObjectKey != null) {
            if (imageObjectKey.isBlank()) {
                throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
            }

            if (!imageObjectKey.matches("^[a-zA-Z0-9/_.-]+$")) {
                throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
            }
        }

        // 3) category: 제공된 경우에만 “허용 목록” 검사
        String category = request.getCategory();
        if (category != null) {
            List<String> allowed = categoryRepository.findAll().stream()
                    .map(Category::getName)
                    .collect(Collectors.toList());
            if (!allowed.contains(category)) {
                throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
            }
        }

        // 4) capacity: 제공된 경우만 1~100 범위 검사
        Integer capacity = request.getCapacity();
        if (capacity != null) {
            if (capacity < 1 || capacity > 100) {
                throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
            }
        }

        // 5) date: 제공된 경우만 미래 시점인지 검사
        LocalDateTime date = request.getDate();
        if (date != null) {
            if (date.isBefore(LocalDateTime.now())) {
                throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
            }
        }

        // 6) location: 제공된 경우만 길이 검사
        String location = request.getLocation();
        if (location != null) {
            if (location.length() < 1 || location.length() > 30) {
                throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
            }
        }

        // 7) openChatUrl: 제공된 경우만 https 검사
        String chatUrl = request.getOpenChatUrl();
        if (chatUrl != null) {
            if (!chatUrl.startsWith("https://")) {
                throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
            }
        }

        // 8) description: 제공된 경우만 800자 제한
        String desc = request.getDescription();
        if (desc != null) {
            if (desc.length() > 800) {
                throw new CustomException(GatheringErrorCode.INVALID_FIELD_VALUE);
            }
        }
    }

    private  void validateKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            throw new CustomException(GatheringErrorCode.INVALID_KEYWORD_FORMAT);
        }
    }

    private String validateParamsFormatAndParseSortParam(int page, int size, String sortParam) {
        validateParamsFormat(page, size);

        String[] parts = sortParam.split(",");
        String property = (parts.length > 0 && !parts[0].isBlank()) ? parts[0] : "joinedAt";
        String dirStr = (parts.length > 1) ? parts[1].toLowerCase() : "asc";

        if (!property.equals("joinedAt")) {
            throw new CustomException(ParticipationErrorCode.INVALID_PARAMETER_FORMAT);
        }

        return dirStr;
    }

    private void validateParamsFormat(int page, int size) {
        if  (page < 1 || size < 1 || size > 12) {
            throw new CustomException(GatheringErrorCode.INVALID_PARAMETER_FORMAT);
        }
    }

    private Category verifyCategoryName(String categoryName) {
        Category category = null;
        if (categoryName != null && !categoryName.isBlank()) {
            category = categoryRepository.findByName(categoryName)
                    .orElseThrow(() -> new CustomException(GatheringErrorCode.CATEGORY_NOT_FOUND));
        }
        return category;
    }
}