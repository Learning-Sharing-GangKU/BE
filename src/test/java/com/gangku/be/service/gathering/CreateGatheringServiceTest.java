package com.gangku.be.service.gathering;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import com.gangku.be.dto.gathering.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.GatheringResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.CategoryErrorCode;
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.GatheringService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreateGatheringServiceTest {

    @Mock
    private GatheringRepository gatheringRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GatheringCreateRequestDto requestDto;

    @InjectMocks
    private GatheringService gatheringService;

    // ===== 공통 유효 요청 스텁 =====
    private void stubCommonValidRequest() {
        when(requestDto.getTitle()).thenReturn("유효한 제목");
        when(requestDto.getImageUrl()).thenReturn("https://example.com/image.png");
        when(requestDto.getCategory()).thenReturn("스터디");
        when(requestDto.getCapacity()).thenReturn(10);
        when(requestDto.getDate()).thenReturn(LocalDateTime.now().plusDays(1));
        when(requestDto.getLocation()).thenReturn("서울");
        when(requestDto.getOpenChatUrl()).thenReturn("https://open.kakao.com/o/abcd");
        when(requestDto.getDescription()).thenReturn("모임 설명입니다.");
    }

    private void stubValidCategorySetup() {
        Category category = new Category();
        // Category 엔티티에 setName 이 있다고 가정 (없으면 builder 등 네 코드에 맞게 수정)
        category.setName("스터디");

        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(categoryRepository.findByName("스터디")).thenReturn(Optional.of(category));
    }

    private User stubHostUser(Long hostId) {
        User host = new User();
        // 굳이 id를 쓰진 않지만, 필요하면 네 엔티티에 맞게 setId 추가
        // host.setId(hostId);
        when(userRepository.findById(hostId)).thenReturn(Optional.of(host));
        return host;
    }

    // =========================================================
    // 1. 정상 케이스들
    // =========================================================

    @Test
    @DisplayName("유효한 요청 → 모임 생성 & 호스트 Participation 저장")
    void createGathering_withValidRequest_createsGatheringAndHostParticipation() {
        // given
        Long hostId = 1L;
        stubCommonValidRequest();
        stubValidCategorySetup();
        User host = stubHostUser(hostId);

        Gathering savedGathering = new Gathering();
        savedGathering.setHost(host);

        when(gatheringRepository.save(any(Gathering.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(participationRepository.save(any(Participation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        GatheringResponseDto response = gatheringService.createGathering(requestDto, hostId);

        // then
        assertNotNull(response, "응답 DTO는 null 이면 안 된다.");
        verify(gatheringRepository, times(1)).save(any(Gathering.class));
        verify(participationRepository, times(1)).save(any(Participation.class));
    }

    @Test
    @DisplayName("imageUrl 이 null 인 경우 → 이미지 없이 모임 생성")
    void createGathering_withNullImageUrl_createsGatheringWithoutImage() {
        // given
        Long hostId = 1L;
        stubCommonValidRequest();
        when(requestDto.getImageUrl()).thenReturn(null); // override

        stubValidCategorySetup();
        User host = stubHostUser(hostId);

        when(gatheringRepository.save(any(Gathering.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(participationRepository.save(any(Participation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        GatheringResponseDto response = gatheringService.createGathering(requestDto, hostId);

        // then
        assertNotNull(response);
        verify(gatheringRepository).save(any(Gathering.class));
        verify(participationRepository).save(any(Participation.class));
    }

    @Test
    @DisplayName("capacity가 최소값(1)인 경우 → 정상 생성")
    void createGathering_withMinCapacity_createsGathering() {
        // given
        Long hostId = 1L;
        stubCommonValidRequest();
        when(requestDto.getCapacity()).thenReturn(1);

        stubValidCategorySetup();
        stubHostUser(hostId);

        when(gatheringRepository.save(any(Gathering.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        GatheringResponseDto response = gatheringService.createGathering(requestDto, hostId);

        // then
        assertNotNull(response);
        verify(gatheringRepository).save(any(Gathering.class));
    }

    @Test
    @DisplayName("capacity가 최대값(100)인 경우 → 정상 생성")
    void createGathering_withMaxCapacity_createsGathering() {
        // given
        Long hostId = 1L;
        stubCommonValidRequest();
        when(requestDto.getCapacity()).thenReturn(100);

        stubValidCategorySetup();
        stubHostUser(hostId);

        when(gatheringRepository.save(any(Gathering.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        GatheringResponseDto response = gatheringService.createGathering(requestDto, hostId);

        // then
        assertNotNull(response);
        verify(gatheringRepository).save(any(Gathering.class));
    }

    // =========================================================
    // 2. 예외 케이스들
    // =========================================================

    @Test
    @DisplayName("존재하지 않는 호스트 ID → USER_NOT_FOUND")
    void createGathering_withNonExistingHost_throwsUserNotFound() {
        // given
        Long hostId = 999L;
        stubCommonValidRequest();
        stubValidCategorySetup();

        when(userRepository.findById(hostId)).thenReturn(Optional.empty());

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> gatheringService.createGathering(requestDto, hostId));

        // then
        assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("카테고리가 DB 목록에 없는 경우 → INVALID_FIELD_VALUE")
    void createGathering_withInvalidCategoryName_throwsInvalidFieldValue() {
        // given
        stubCommonValidRequest();
        when(requestDto.getCategory()).thenReturn("없는카테고리");

        Category existing = new Category();
        existing.setName("스터디");
        when(categoryRepository.findAll()).thenReturn(List.of(existing));

        Long hostId = 1L; // userRepository는 호출되기 전에 터지므로 스텁 불필요

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> gatheringService.createGathering(requestDto, hostId));

        // then
        assertEquals(GatheringErrorCode.INVALID_FIELD_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("허용된 카테고리지만 findByName 에서 조회 실패 → CATEGORY_NOT_FOUND")
    void createGathering_withCategoryMissingInFindByName_throwsCategoryNotFound() {
        // given
        stubCommonValidRequest();
        when(requestDto.getCategory()).thenReturn("스터디");

        Category category = new Category();
        category.setName("스터디");
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(categoryRepository.findByName("스터디")).thenReturn(Optional.empty());

        Long hostId = 1L;
        stubHostUser(hostId);

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> gatheringService.createGathering(requestDto, hostId));

        // then
        assertEquals(CategoryErrorCode.CATEGORY_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("title 이 null 또는 빈 문자열 → INVALID_FIELD_VALUE")
    void createGathering_withEmptyTitle_throwsInvalidFieldValue() {
        // given
        stubCommonValidRequest();
        when(requestDto.getTitle()).thenReturn("");

        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        Long hostId = 1L;

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> gatheringService.createGathering(requestDto, hostId));

        // then
        assertEquals(GatheringErrorCode.INVALID_FIELD_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("title 길이 31자 이상 → INVALID_FIELD_VALUE")
    void createGathering_withTooLongTitle_throwsInvalidFieldValue() {
        // given
        stubCommonValidRequest();
        when(requestDto.getTitle()).thenReturn("a".repeat(31));

        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

        Long hostId = 1L;

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> gatheringService.createGathering(requestDto, hostId));

        // then
        assertEquals(GatheringErrorCode.INVALID_FIELD_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("imageUrl 이 URL 형식이 아닌 경우 → INVALID_FIELD_VALUE")
    void createGathering_withInvalidImageUrl_throwsInvalidFieldValue() {
        // given
        when(requestDto.getTitle()).thenReturn("축구");
        when(requestDto.getImageUrl()).thenReturn("htp://not-valid-url"); // UrlValidator 에서 invalid 로 볼 형식
        when(requestDto.getCategory()).thenReturn("운동");
        when(requestDto.getCapacity()).thenReturn(10);
        when(requestDto.getDate()).thenReturn(LocalDateTime.now().plusDays(1));
        when(requestDto.getLocation()).thenReturn("건대입구역");
        when(requestDto.getOpenChatUrl()).thenReturn("https://open.kakao.com/o/abcd1234");
        when(requestDto.getDescription()).thenReturn("설명");

        // imageUrl 검증에서 예외 발생
        CustomException ex = assertThrows(CustomException.class,
                () -> gatheringService.createGathering(requestDto, 1L));

        assertEquals(GatheringErrorCode.INVALID_FIELD_VALUE, ex.getErrorCode());
        verifyNoInteractions(userRepository, gatheringRepository, participationRepository);
    }

    @Test
    @DisplayName("capacity < 1 → INVALID_FIELD_VALUE")
    void createGathering_withTooSmallCapacity_throwsInvalidFieldValue() {
        // given
        stubCommonValidRequest();
        when(requestDto.getCapacity()).thenReturn(0);

        Category category = new Category();
        category.setName("스터디");
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        Long hostId = 1L;

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> gatheringService.createGathering(requestDto, hostId));

        // then
        assertEquals(GatheringErrorCode.INVALID_FIELD_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("capacity > 100 → INVALID_FIELD_VALUE")
    void createGathering_withTooLargeCapacity_throwsInvalidFieldValue() {
        // given
        stubCommonValidRequest();
        when(requestDto.getCapacity()).thenReturn(101);

        Category category = new Category();
        category.setName("스터디");
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        Long hostId = 1L;

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> gatheringService.createGathering(requestDto, hostId));

        // then
        assertEquals(GatheringErrorCode.INVALID_FIELD_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("date 가 null → INVALID_FIELD_VALUE")
    void createGathering_withNullDate_throwsInvalidFieldValue() {
        // given
        stubCommonValidRequest();
        when(requestDto.getDate()).thenReturn(null);

        Category category = new Category();
        category.setName("스터디");
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        Long hostId = 1L;

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> gatheringService.createGathering(requestDto, hostId));

        // then
        assertEquals(GatheringErrorCode.INVALID_FIELD_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("date 가 과거 시간 → INVALID_FIELD_VALUE")
    void createGathering_withPastDate_throwsInvalidFieldValue() {
        // given
        stubCommonValidRequest();
        when(requestDto.getDate()).thenReturn(LocalDateTime.now().minusDays(1));

        Category category = new Category();
        category.setName("스터디");
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        Long hostId = 1L;

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> gatheringService.createGathering(requestDto, hostId));

        // then
        assertEquals(GatheringErrorCode.INVALID_FIELD_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("location 이 null 또는 빈 문자열 → INVALID_FIELD_VALUE")
    void createGathering_withEmptyLocation_throwsInvalidFieldValue() {
        // given
        stubCommonValidRequest();
        when(requestDto.getLocation()).thenReturn("");

        Category category = new Category();
        category.setName("스터디");
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        Long hostId = 1L;

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> gatheringService.createGathering(requestDto, hostId));

        // then
        assertEquals(GatheringErrorCode.INVALID_FIELD_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("location 길이 31자 이상 → INVALID_FIELD_VALUE")
    void createGathering_withTooLongLocation_throwsInvalidFieldValue() {
        // given
        stubCommonValidRequest();
        when(requestDto.getLocation()).thenReturn("a".repeat(31));

        Category category = new Category();
        category.setName("스터디");
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        Long hostId = 1L;

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> gatheringService.createGathering(requestDto, hostId));

        // then
        assertEquals(GatheringErrorCode.INVALID_FIELD_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("openChatUrl 이 null → INVALID_FIELD_VALUE")
    void createGathering_withNullOpenChatUrl_throwsInvalidFieldValue() {
        // given
        stubCommonValidRequest();
        when(requestDto.getOpenChatUrl()).thenReturn(null);

        Category category = new Category();
        category.setName("스터디");
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        Long hostId = 1L;

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> gatheringService.createGathering(requestDto, hostId));

        // then
        assertEquals(GatheringErrorCode.INVALID_FIELD_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("openChatUrl 이 https:// 로 시작하지 않음 → INVALID_FIELD_VALUE")
    void createGathering_withNonHttpsOpenChatUrl_throwsInvalidFieldValue() {
        // given
        stubCommonValidRequest();
        when(requestDto.getOpenChatUrl()).thenReturn("http://example.com/openchat");

        Category category = new Category();
        category.setName("스터디");
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        Long hostId = 1L;

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> gatheringService.createGathering(requestDto, hostId));

        // then
        assertEquals(GatheringErrorCode.INVALID_FIELD_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("description 이 null → INVALID_FIELD_VALUE")
    void createGathering_withNullDescription_throwsInvalidFieldValue() {
        // given
        stubCommonValidRequest();
        when(requestDto.getDescription()).thenReturn(null);

        Category category = new Category();
        category.setName("스터디");
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        Long hostId = 1L;

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> gatheringService.createGathering(requestDto, hostId));

        // then
        assertEquals(GatheringErrorCode.INVALID_FIELD_VALUE, ex.getErrorCode());
    }

    @Test
    @DisplayName("description 길이 801자 이상 → INVALID_FIELD_VALUE")
    void createGathering_withTooLongDescription_throwsInvalidFieldValue() {
        // given
        stubCommonValidRequest();
        when(requestDto.getDescription()).thenReturn("a".repeat(801));

        Category category = new Category();
        category.setName("스터디");
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        Long hostId = 1L;

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> gatheringService.createGathering(requestDto, hostId));

        // then
        assertEquals(GatheringErrorCode.INVALID_FIELD_VALUE, ex.getErrorCode());
    }

    // =========================================================
    // 3. 경계 케이스들
    // =========================================================

    @Test
    @DisplayName("title 길이 1자 → 정상 생성")
    void createGathering_withTitleLengthOne_succeeds() {
        // given
        Long hostId = 1L;
        stubCommonValidRequest();
        when(requestDto.getTitle()).thenReturn("가");

        stubValidCategorySetup();
        stubHostUser(hostId);

        when(gatheringRepository.save(any(Gathering.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        GatheringResponseDto response = gatheringService.createGathering(requestDto, hostId);

        // then
        assertNotNull(response);
    }

    @Test
    @DisplayName("title 길이 30자 → 정상 생성")
    void createGathering_withTitleLengthThirty_succeeds() {
        // given
        Long hostId = 1L;
        stubCommonValidRequest();
        when(requestDto.getTitle()).thenReturn("a".repeat(30));

        stubValidCategorySetup();
        stubHostUser(hostId);

        when(gatheringRepository.save(any(Gathering.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        GatheringResponseDto response = gatheringService.createGathering(requestDto, hostId);

        // then
        assertNotNull(response);
    }

    @Test
    @DisplayName("description 길이 800자 → 정상 생성")
    void createGathering_withDescriptionLengthEightHundred_succeeds() {
        // given
        Long hostId = 1L;
        stubCommonValidRequest();
        when(requestDto.getDescription()).thenReturn("a".repeat(800));

        stubValidCategorySetup();
        stubHostUser(hostId);

        when(gatheringRepository.save(any(Gathering.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        GatheringResponseDto response = gatheringService.createGathering(requestDto, hostId);

        // then
        assertNotNull(response);
    }

    @Test
    @DisplayName("date 가 현재보다 조금 미래(now+1분) → 정상 생성")
    void createGathering_withNearFutureDate_succeeds() {
        // given
        Long hostId = 1L;
        stubCommonValidRequest();
        when(requestDto.getDate()).thenReturn(LocalDateTime.now().plusMinutes(1));

        stubValidCategorySetup();
        stubHostUser(hostId);

        when(gatheringRepository.save(any(Gathering.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        GatheringResponseDto response = gatheringService.createGathering(requestDto, hostId);

        // then
        assertNotNull(response);
    }

    @Test
    @DisplayName("openChatUrl 이 https:// 로 시작하는 경우 → 정상 생성")
    void createGathering_withHttpsOpenChatUrl_succeeds() {
        // given
        Long hostId = 1L;
        stubCommonValidRequest();
        when(requestDto.getOpenChatUrl()).thenReturn("https://example.com/openchat");

        stubValidCategorySetup();
        stubHostUser(hostId);

        when(gatheringRepository.save(any(Gathering.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        GatheringResponseDto response = gatheringService.createGathering(requestDto, hostId);

        // then
        assertNotNull(response);
    }
}
