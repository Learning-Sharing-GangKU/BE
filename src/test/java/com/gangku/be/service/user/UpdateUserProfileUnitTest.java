package com.gangku.be.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.User;
import com.gangku.be.dto.ai.request.TextFilterRequestDto;
import com.gangku.be.dto.ai.response.TextFilterResponseDto;
import com.gangku.be.dto.user.UserProfileUpdateRequestDto;
import com.gangku.be.dto.user.UserProfileUpdateResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.external.ai.AiApiClient;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.PreferredCategoryRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.UserService;
import com.gangku.be.util.ai.AiTextFilterMapper;
import com.gangku.be.util.object.FileUrlResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class UpdateUserProfileUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private PreferredCategoryRepository preferredCategoryRepository;
    @Mock private FileUrlResolver fileUrlResolver;
    @Mock private AiApiClient aiApiClient;
    @Mock private AiTextFilterMapper aiTextFilterMapper;

    @InjectMocks private UserService userService;

    @Test
    @DisplayName("프로필 수정 (200 OK): 본인 요청이면 프로필 수정 성공")
    void updateUserProfile_success() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 1L;

        User user =
                User.builder()
                        .id(targetUserId)
                        .email("test@example.com")
                        .password("encoded-password")
                        .nickname("기존닉네임")
                        .age(23)
                        .gender("FEMALE")
                        .enrollNumber(22)
                        .profileImageObjectKey("old/profile.png")
                        .reviewPublic(true)
                        .preferredCategories(new ArrayList<>())
                        .build();

        UserProfileUpdateRequestDto requestDto =
                new UserProfileUpdateRequestDto(
                        "new/profile.png", "새로운닉네임", 24, "MALE", 20, List.of("SPORTS", "MUSIC"));

        Category sports = mock(Category.class);
        Category music = mock(Category.class);

        TextFilterRequestDto textFilterRequestDto = mock(TextFilterRequestDto.class);
        TextFilterResponseDto textFilterResponseDto = mock(TextFilterResponseDto.class);

        when(sports.getName()).thenReturn("SPORTS");
        when(music.getName()).thenReturn("MUSIC");

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user));
        when(userRepository.existsByNicknameAndIdNot("새로운닉네임", targetUserId)).thenReturn(false);
        when(categoryRepository.findByNameIn(List.of("SPORTS", "MUSIC")))
                .thenReturn(List.of(sports, music));
        when(userRepository.save(user)).thenReturn(user);
        when(fileUrlResolver.toPublicUrl("new/profile.png"))
                .thenReturn("https://cdn.example.com/profiles/2025/09/uuid.jpg");
        when(aiTextFilterMapper.fromProfileUpdate(requestDto)).thenReturn(textFilterRequestDto);
        when(aiApiClient.filterText(textFilterRequestDto)).thenReturn(textFilterResponseDto);
        when(textFilterResponseDto.isAllowed()).thenReturn(true);

        // when
        UserProfileUpdateResponseDto response =
                userService.updateUserProfile(targetUserId, currentUserId, requestDto);

        // then
        assertThat(response.getId()).isEqualTo("usr_1");
        assertThat(response.getProfileImageUrl())
                .isEqualTo("https://cdn.example.com/profiles/2025/09/uuid.jpg");
        assertThat(response.getNickname()).isEqualTo("새로운닉네임");
        assertThat(response.getAge()).isEqualTo(24);
        assertThat(response.getGender()).isEqualTo("MALE");
        assertThat(response.getEnrollNumber()).isEqualTo(20);
        assertThat(response.getPreferredCategories()).containsExactly("SPORTS", "MUSIC");
        assertThat(response.getUpdatedAt()).isEqualTo(user.getUpdatedAt());

        verify(userRepository, times(1)).findById(targetUserId);
        verify(userRepository, times(1)).existsByNicknameAndIdNot("새로운닉네임", targetUserId);
        verify(categoryRepository, times(1)).findByNameIn(List.of("SPORTS", "MUSIC"));
        verify(userRepository, times(1)).flush();
        verify(preferredCategoryRepository, times(1)).saveAll(anyList());
        verify(userRepository, times(1)).save(user);
        verify(fileUrlResolver, times(1)).toPublicUrl("new/profile.png");
        verify(aiTextFilterMapper, times(1)).fromProfileUpdate(requestDto);
        verify(aiApiClient, times(1)).filterText(textFilterRequestDto);
        verifyNoMoreInteractions(
                userRepository,
                categoryRepository,
                preferredCategoryRepository,
                fileUrlResolver,
                aiApiClient,
                aiTextFilterMapper);
    }

    @Test
    @DisplayName("프로필 수정 (400 Bad Request): 닉네임에 금칙어가 있으면 INVALID_NICKNAME 예외")
    void updateUserProfile_invalidNickname() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 1L;

        User user =
                User.builder()
                        .id(targetUserId)
                        .email("test@example.com")
                        .password("encoded-password")
                        .nickname("기존닉네임")
                        .preferredCategories(new ArrayList<>())
                        .build();

        UserProfileUpdateRequestDto requestDto =
                new UserProfileUpdateRequestDto(
                        null, "금칙어닉네임", 24, "MALE", 20, List.of("SPORTS"));

        TextFilterRequestDto textFilterRequestDto = mock(TextFilterRequestDto.class);
        TextFilterResponseDto textFilterResponseDto = mock(TextFilterResponseDto.class);

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user));
        when(aiTextFilterMapper.fromProfileUpdate(requestDto)).thenReturn(textFilterRequestDto);
        when(aiApiClient.filterText(textFilterRequestDto)).thenReturn(textFilterResponseDto);
        when(textFilterResponseDto.isAllowed()).thenReturn(false);

        // when
        assertThatThrownBy(() -> userService.updateUserProfile(targetUserId, currentUserId, requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.INVALID_NICKNAME);

        // then
        verify(userRepository, times(1)).findById(targetUserId);
        verify(aiTextFilterMapper, times(1)).fromProfileUpdate(requestDto);
        verify(aiApiClient, times(1)).filterText(textFilterRequestDto);
        verify(userRepository, never()).existsByNicknameAndIdNot(anyString(), anyLong());
        verify(userRepository, never()).flush();
        verify(userRepository, never()).save(any());

        verifyNoInteractions(categoryRepository, preferredCategoryRepository, fileUrlResolver);
        verifyNoMoreInteractions(userRepository, aiApiClient, aiTextFilterMapper);
    }

    @Test
    @DisplayName("프로필 수정 (404 Not Found): 대상 유저가 없으면 USER_NOT_FOUND 예외")
    void updateUserProfile_userNotFound() {
        // given
        Long targetUserId = 999L;
        Long currentUserId = 999L;

        UserProfileUpdateRequestDto requestDto =
                new UserProfileUpdateRequestDto(
                        null, "새로운닉네임", 24, "MALE", 20, List.of("SPORTS", "MUSIC"));

        when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

        // when
        assertThatThrownBy(
                        () ->
                                userService.updateUserProfile(
                                        targetUserId, currentUserId, requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        // then
        verify(userRepository, times(1)).findById(targetUserId);
        verifyNoInteractions(categoryRepository, preferredCategoryRepository, fileUrlResolver);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("프로필 수정 (403 Forbidden): 본인이 아니면 NO_PERMISSION_TO_UPDATE_PROFILE 예외")
    void updateUserProfile_noPermission() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 2L;

        User user =
                User.builder()
                        .id(targetUserId)
                        .email("test@example.com")
                        .password("encoded-password")
                        .nickname("기존닉네임")
                        .preferredCategories(new ArrayList<>())
                        .build();

        UserProfileUpdateRequestDto requestDto =
                new UserProfileUpdateRequestDto(null, "새로운닉네임", 24, "MALE", 20, List.of("SPORTS"));

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user));

        // when
        assertThatThrownBy(
                        () ->
                                userService.updateUserProfile(
                                        targetUserId, currentUserId, requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.NO_PERMISSION_TO_UPDATE_PROFILE);

        // then
        verify(userRepository, times(1)).findById(targetUserId);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(categoryRepository, preferredCategoryRepository, fileUrlResolver);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("프로필 수정 (409 Conflict): 닉네임이 중복되면 NICKNAME_ALREADY_EXISTS 예외")
    void updateUserProfile_nicknameConflict() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 1L;

        User user =
                User.builder()
                        .id(targetUserId)
                        .email("test@example.com")
                        .password("encoded-password")
                        .nickname("기존닉네임")
                        .preferredCategories(new ArrayList<>())
                        .build();

        UserProfileUpdateRequestDto requestDto =
                new UserProfileUpdateRequestDto(null, "중복닉네임", 24, "MALE", 20, null);

        TextFilterRequestDto textFilterRequestDto = mock(TextFilterRequestDto.class);
        TextFilterResponseDto textFilterResponseDto = mock(TextFilterResponseDto.class);

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user));
        when(aiTextFilterMapper.fromProfileUpdate(requestDto)).thenReturn(textFilterRequestDto);
        when(aiApiClient.filterText(textFilterRequestDto)).thenReturn(textFilterResponseDto);
        when(textFilterResponseDto.isAllowed()).thenReturn(true);
        when(userRepository.existsByNicknameAndIdNot("중복닉네임", targetUserId)).thenReturn(true);

        // when
        assertThatThrownBy(
                () ->
                        userService.updateUserProfile(
                                targetUserId, currentUserId, requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.NICKNAME_ALREADY_EXISTS);

        // then
        verify(userRepository, times(1)).findById(targetUserId);
        verify(aiTextFilterMapper, times(1)).fromProfileUpdate(requestDto);
        verify(aiApiClient, times(1)).filterText(textFilterRequestDto);
        verify(userRepository, times(1)).existsByNicknameAndIdNot("중복닉네임", targetUserId);
        verify(userRepository, never()).save(any());

        verifyNoInteractions(categoryRepository, preferredCategoryRepository, fileUrlResolver);
        verifyNoMoreInteractions(userRepository, aiTextFilterMapper, aiApiClient);
    }
}
