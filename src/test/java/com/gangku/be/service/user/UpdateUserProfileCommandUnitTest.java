package com.gangku.be.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.User;
import com.gangku.be.dto.user.UserProfileUpdateRequestDto;
import com.gangku.be.dto.user.UserProfileUpdateResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.PreferredCategoryRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.command.UserCommandService;
import com.gangku.be.support.UserLookup;
import com.gangku.be.util.object.FileUrlResolver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class UpdateUserProfileCommandUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private UserLookup userLookup;
    @Mock private CategoryRepository categoryRepository;
    @Mock private PreferredCategoryRepository preferredCategoryRepository;
    @Mock private FileUrlResolver fileUrlResolver;

    @InjectMocks private UserCommandService userCommandService;

    @Test
    @DisplayName("프로필 수정 저장 성공: 본인 요청이면 프로필 수정 성공")
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
        when(sports.getName()).thenReturn("SPORTS");
        when(music.getName()).thenReturn("MUSIC");

        when(userLookup.findById(targetUserId)).thenReturn(user);
        when(userRepository.existsByNicknameAndIdNot("새로운닉네임", targetUserId)).thenReturn(false);
        when(categoryRepository.findByNameIn(List.of("SPORTS", "MUSIC")))
                .thenReturn(List.of(sports, music));
        when(userRepository.save(user)).thenReturn(user);
        when(fileUrlResolver.toPublicUrl("new/profile.png"))
                .thenReturn("https://cdn.example.com/profiles/2025/09/uuid.jpg");

        // when
        UserProfileUpdateResponseDto result =
                userCommandService.updateUserProfile(targetUserId, currentUserId, requestDto);

        // then
        assertThat(result.getId()).isEqualTo("usr_1");
        assertThat(result.getNickname()).isEqualTo("새로운닉네임");
        assertThat(result.getProfileImageUrl())
                .isEqualTo("https://cdn.example.com/profiles/2025/09/uuid.jpg");
        assertThat(result.getAge()).isEqualTo(24);
        assertThat(result.getGender()).isEqualTo("MALE");
        assertThat(result.getEnrollNumber()).isEqualTo(20);
        assertThat(result.getPreferredCategories()).containsExactly("SPORTS", "MUSIC");

        verify(userLookup, times(1)).findById(targetUserId);
        verify(userRepository, times(1)).existsByNicknameAndIdNot("새로운닉네임", targetUserId);
        verify(userRepository, times(1)).flush();
        verify(categoryRepository, times(1)).findByNameIn(List.of("SPORTS", "MUSIC"));
        verify(preferredCategoryRepository, times(1)).saveAll(anyList());
        verify(userRepository, times(1)).save(user);
        verify(fileUrlResolver, times(1)).toPublicUrl("new/profile.png");

        verifyNoMoreInteractions(
                userLookup,
                userRepository,
                categoryRepository,
                preferredCategoryRepository,
                fileUrlResolver);
    }

    @Test
    @DisplayName("프로필 수정 실패 (404 Not Found): 대상 유저가 없으면 USER_NOT_FOUND 예외")
    void updateUserProfile_userNotFound() {
        // given
        Long targetUserId = 999L;
        Long currentUserId = 999L;

        UserProfileUpdateRequestDto requestDto =
                new UserProfileUpdateRequestDto(
                        null, "새로운닉네임", 24, "MALE", 20, List.of("SPORTS", "MUSIC"));

        when(userLookup.findById(targetUserId))
                .thenThrow(new CustomException(UserErrorCode.USER_NOT_FOUND));

        // when & then
        assertThatThrownBy(
                        () ->
                                userCommandService.updateUserProfile(
                                        targetUserId, currentUserId, requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        verify(userLookup, times(1)).findById(targetUserId);
        verifyNoInteractions(categoryRepository, preferredCategoryRepository, fileUrlResolver);
        verifyNoMoreInteractions(userLookup);
    }

    @Test
    @DisplayName("프로필 수정 실패 (403 Forbidden): 본인이 아니면 NO_PERMISSION_TO_UPDATE_PROFILE 예외")
    void updateUserProfile_noPermission() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 2L;

        User user =
                User.builder()
                        .id(targetUserId)
                        .email("test@example.com")
                        .nickname("기존닉네임")
                        .preferredCategories(new ArrayList<>())
                        .build();

        UserProfileUpdateRequestDto requestDto =
                new UserProfileUpdateRequestDto(null, "새로운닉네임", 24, "MALE", 20, List.of("SPORTS"));

        when(userLookup.findById(targetUserId)).thenReturn(user);

        // when & then
        assertThatThrownBy(
                        () ->
                                userCommandService.updateUserProfile(
                                        targetUserId, currentUserId, requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.NO_PERMISSION_TO_UPDATE_PROFILE);

        verify(userLookup, times(1)).findById(targetUserId);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(categoryRepository, preferredCategoryRepository, fileUrlResolver);
        verifyNoMoreInteractions(userLookup);
    }

    @Test
    @DisplayName("프로필 수정 실패 (409 Conflict): 닉네임이 중복되면 NICKNAME_ALREADY_EXISTS 예외")
    void updateUserProfile_nicknameConflict() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 1L;

        User user =
                User.builder()
                        .id(targetUserId)
                        .email("test@example.com")
                        .nickname("기존닉네임")
                        .preferredCategories(new ArrayList<>())
                        .build();

        UserProfileUpdateRequestDto requestDto =
                new UserProfileUpdateRequestDto(null, "중복닉네임", 24, "MALE", 20, null);

        when(userLookup.findById(targetUserId)).thenReturn(user);
        when(userRepository.existsByNicknameAndIdNot("중복닉네임", targetUserId)).thenReturn(true);

        // when & then
        assertThatThrownBy(
                        () ->
                                userCommandService.updateUserProfile(
                                        targetUserId, currentUserId, requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.NICKNAME_ALREADY_EXISTS);

        verify(userLookup, times(1)).findById(targetUserId);
        verify(userRepository, times(1)).existsByNicknameAndIdNot("중복닉네임", targetUserId);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(categoryRepository, preferredCategoryRepository, fileUrlResolver);
        verifyNoMoreInteractions(userLookup);
    }
}
