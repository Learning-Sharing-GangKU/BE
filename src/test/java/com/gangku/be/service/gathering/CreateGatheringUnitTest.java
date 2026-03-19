package com.gangku.be.service.gathering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.Gathering;
import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import com.gangku.be.dto.ai.request.TextFilterRequestDto;
import com.gangku.be.dto.ai.response.TextFilterResponseDto;
import com.gangku.be.dto.gathering.request.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.response.GatheringResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.CategoryErrorCode;
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.external.ai.AiApiClient;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.GatheringRepository;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.GatheringService;
import com.gangku.be.util.ai.AiTextFilterMapper;
import com.gangku.be.util.object.FileUrlResolver;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class CreateGatheringUnitTest {

    @Mock private GatheringRepository gatheringRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ParticipationRepository participationRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileUrlResolver fileUrlResolver;
    @Mock private AiApiClient aiApiClient;
    @Mock private AiTextFilterMapper aiTextFilterMapper;

    @InjectMocks private GatheringService gatheringService;

    @Test
    @DisplayName("모임 생성 (201 Created): 유저/카테고리 유효 + 금칙어 없으면 모임 생성 성공")
    void createGathering_success() {
        // given
        Long hostId = 1L;

        GatheringCreateRequestDto requestDto =
                new GatheringCreateRequestDto(
                        "알고리즘 스터디",
                        "statics/image/prod/2025/11/efe6-a7d.jpg",
                        "study",
                        12,
                        LocalDateTime.of(2025, 10, 1, 10, 0),
                        "공학관 301",
                        "https://open.kakao.com/o/abcdef",
                        "기초부터 차근차근 알고리즘을 공부합니다.");

        User host = User.builder().id(hostId).participations(new ArrayList<>()).build();

        Category category = mock(Category.class);

        TextFilterRequestDto textFilterRequestDto = mock(TextFilterRequestDto.class);
        TextFilterResponseDto textFilterResponseDto = mock(TextFilterResponseDto.class);

        when(category.getName()).thenReturn("study");

        when(userRepository.findById(hostId)).thenReturn(Optional.of(host));
        when(categoryRepository.findByName("study")).thenReturn(Optional.of(category));

        when(aiTextFilterMapper.fromGatheringCreate(requestDto)).thenReturn(textFilterRequestDto);
        when(aiApiClient.filterText(textFilterRequestDto)).thenReturn(textFilterResponseDto);
        when(textFilterResponseDto.isAllowed()).thenReturn(true);

        when(gatheringRepository.save(any(Gathering.class)))
                .thenAnswer(
                        inv -> {
                            Gathering g = inv.getArgument(0);
                            java.lang.reflect.Field idField =
                                    Gathering.class.getDeclaredField("id");
                            idField.setAccessible(true);
                            idField.set(g, 12345L);
                            return g;
                        });

        when(fileUrlResolver.toPublicUrl("statics/image/prod/2025/11/efe6-a7d.jpg"))
                .thenReturn("https://cdn.example.com/gatherings/2025/09/cover-uuid.jpg");

        // when
        GatheringResponseDto response = gatheringService.createGathering(requestDto, hostId);

        // then
        ArgumentCaptor<Gathering> gatheringCaptor = ArgumentCaptor.forClass(Gathering.class);
        verify(gatheringRepository, times(1)).save(gatheringCaptor.capture());
        Gathering savedGathering = gatheringCaptor.getValue();

        assertThat(savedGathering.getHost()).isEqualTo(host);
        assertThat(savedGathering.getCategory()).isEqualTo(category);
        assertThat(savedGathering.getTitle()).isEqualTo("알고리즘 스터디");
        assertThat(savedGathering.getGatheringImageObjectKey())
                .isEqualTo("statics/image/prod/2025/11/efe6-a7d.jpg");
        assertThat(savedGathering.getCapacity()).isEqualTo(12);
        assertThat(savedGathering.getDate()).isEqualTo(LocalDateTime.of(2025, 10, 1, 10, 0));
        assertThat(savedGathering.getLocation()).isEqualTo("공학관 301");
        assertThat(savedGathering.getOpenChatUrl()).isEqualTo("https://open.kakao.com/o/abcdef");
        assertThat(savedGathering.getDescription()).isEqualTo("기초부터 차근차근 알고리즘을 공부합니다.");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("gath_12345");
        assertThat(response.getTitle()).isEqualTo("알고리즘 스터디");
        assertThat(response.getGatheringImageUrl())
                .isEqualTo("https://cdn.example.com/gatherings/2025/09/cover-uuid.jpg");
        assertThat(response.getCategory()).isEqualTo("study");
        assertThat(response.getCapacity()).isEqualTo(12);
        assertThat(response.getLocation()).isEqualTo("공학관 301");
        assertThat(response.getOpenChatUrl()).isEqualTo("https://open.kakao.com/o/abcdef");
        assertThat(response.getDescription()).isEqualTo("기초부터 차근차근 알고리즘을 공부합니다.");

        verify(userRepository, times(1)).findById(hostId);
        verify(categoryRepository, times(1)).findByName("study");
        verify(aiTextFilterMapper, times(1)).fromGatheringCreate(requestDto);
        verify(aiApiClient, times(1)).filterText(textFilterRequestDto);
        verify(participationRepository, times(1)).save(any(Participation.class));
        verify(fileUrlResolver, times(1)).toPublicUrl("statics/image/prod/2025/11/efe6-a7d.jpg");

        verifyNoMoreInteractions(
                userRepository,
                categoryRepository,
                gatheringRepository,
                participationRepository,
                fileUrlResolver,
                aiApiClient,
                aiTextFilterMapper);
        verifyNoInteractions(aiApiClient);
    }

    @Test
    @DisplayName("모임 생성 (404 Not Found): 호스트 유저가 없으면 USER_NOT_FOUND 예외")
    void createGathering_userNotFound() {
        // given
        Long hostId = 1L;

        GatheringCreateRequestDto requestDto =
                new GatheringCreateRequestDto(
                        "알고리즘 스터디",
                        "statics/image/prod/2025/11/efe6-a7d.jpg",
                        "study",
                        12,
                        LocalDateTime.of(2025, 10, 1, 10, 0),
                        "공학관 301",
                        "https://open.kakao.com/o/abcdef",
                        "기초부터 차근차근 알고리즘을 공부합니다.");

        when(userRepository.findById(hostId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> gatheringService.createGathering(requestDto, hostId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        verify(userRepository, times(1)).findById(hostId);

        verifyNoInteractions(
                categoryRepository,
                gatheringRepository,
                participationRepository,
                fileUrlResolver,
                aiApiClient,
                aiTextFilterMapper,
                aiApiClient);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("모임 생성 (404 Not Found): 카테고리가 없으면 CATEGORY_NOT_FOUND 예외")
    void createGathering_categoryNotFound() {
        // given
        Long hostId = 1L;

        GatheringCreateRequestDto requestDto =
                new GatheringCreateRequestDto(
                        "알고리즘 스터디",
                        "statics/image/prod/2025/11/efe6-a7d.jpg",
                        "study",
                        12,
                        LocalDateTime.of(2025, 10, 1, 10, 0),
                        "공학관 301",
                        "https://open.kakao.com/o/abcdef",
                        "기초부터 차근차근 알고리즘을 공부합니다.");

        User host = User.builder().id(hostId).build();

        when(userRepository.findById(hostId)).thenReturn(Optional.of(host));
        when(categoryRepository.findByName("study")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> gatheringService.createGathering(requestDto, hostId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(CategoryErrorCode.CATEGORY_NOT_FOUND);

        verify(userRepository, times(1)).findById(hostId);
        verify(categoryRepository, times(1)).findByName("study");

        verifyNoInteractions(
                gatheringRepository,
                participationRepository,
                fileUrlResolver,
                aiApiClient,
                aiTextFilterMapper,
                aiApiClient);
        verifyNoMoreInteractions(userRepository, categoryRepository);
    }

    @Test
    @DisplayName("모임 생성 (400 Bad Request): 제목 또는 설명에 금칙어가 있으면 INVALID_GATHERING_CONTENT 예외")
    void createGathering_invalidContent() {
        // given
        Long hostId = 1L;

        GatheringCreateRequestDto requestDto =
                new GatheringCreateRequestDto(
                        "부적절한 모임 제목",
                        "statics/image/prod/2025/11/efe6-a7d.jpg",
                        "study",
                        12,
                        LocalDateTime.of(2025, 10, 1, 10, 0),
                        "공학관 301",
                        "https://open.kakao.com/o/abcdef",
                        "부적절한 설명");

        User host = User.builder().id(hostId).build();
        Category category = mock(Category.class);

        TextFilterRequestDto textFilterRequestDto = mock(TextFilterRequestDto.class);
        TextFilterResponseDto textFilterResponseDto = mock(TextFilterResponseDto.class);

        when(userRepository.findById(hostId)).thenReturn(Optional.of(host));
        when(categoryRepository.findByName("study")).thenReturn(Optional.of(category));

        when(aiTextFilterMapper.fromGatheringCreate(requestDto)).thenReturn(textFilterRequestDto);
        when(aiApiClient.filterText(textFilterRequestDto)).thenReturn(textFilterResponseDto);
        when(textFilterResponseDto.isAllowed()).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> gatheringService.createGathering(requestDto, hostId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(GatheringErrorCode.INVALID_GATHERING_CONTENT);

        verify(userRepository, times(1)).findById(hostId);
        verify(categoryRepository, times(1)).findByName("study");
        verify(aiTextFilterMapper, times(1)).fromGatheringCreate(requestDto);
        verify(aiApiClient, times(1)).filterText(textFilterRequestDto);

        verify(gatheringRepository, never()).save(any());
        verify(participationRepository, never()).save(any());
        verify(fileUrlResolver, never()).toPublicUrl(anyString());

        verifyNoMoreInteractions(
                userRepository,
                categoryRepository,
                aiTextFilterMapper,
                aiApiClient,
                gatheringRepository,
                participationRepository,
                fileUrlResolver);
        verifyNoInteractions(aiApiClient);
    }
}
