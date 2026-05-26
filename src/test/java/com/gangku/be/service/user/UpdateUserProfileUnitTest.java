package com.gangku.be.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.gangku.be.dto.ai.request.TextFilterRequestDto;
import com.gangku.be.dto.ai.response.TextFilterResponseDto;
import com.gangku.be.dto.user.UserProfileUpdateRequestDto;
import com.gangku.be.dto.user.UserProfileUpdateResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.external.ai.AiApiClient;
import com.gangku.be.service.UserService;
import com.gangku.be.service.command.UserCommandService;
import com.gangku.be.util.ai.AiTextFilterMapper;
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
public class UpdateUserProfileUnitTest {

    @Mock private AiApiClient aiApiClient;
    @Mock private AiTextFilterMapper aiTextFilterMapper;
    @Mock private UserCommandService userCommandService;

    @InjectMocks private UserService userService;

    @Test
    @DisplayName("프로필 수정 (200 OK): 금칙어 없으면 프로필 수정 성공")
    void updateUserProfile_success() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 1L;

        UserProfileUpdateRequestDto requestDto =
                new UserProfileUpdateRequestDto(
                        "new/profile.png", "새로운닉네임", 24, "MALE", 20, List.of("SPORTS", "MUSIC"));

        TextFilterRequestDto textFilterRequestDto = mock(TextFilterRequestDto.class);
        TextFilterResponseDto textFilterResponseDto = mock(TextFilterResponseDto.class);
        UserProfileUpdateResponseDto expectedResponse = mock(UserProfileUpdateResponseDto.class);

        when(aiTextFilterMapper.fromProfileUpdate(requestDto)).thenReturn(textFilterRequestDto);
        when(aiApiClient.filterText(textFilterRequestDto)).thenReturn(textFilterResponseDto);
        when(textFilterResponseDto.isAllowed()).thenReturn(true);
        when(userCommandService.updateUserProfile(targetUserId, currentUserId, requestDto))
                .thenReturn(expectedResponse);

        // when
        UserProfileUpdateResponseDto result =
                userService.updateUserProfile(targetUserId, currentUserId, requestDto);

        // then
        assertThat(result).isEqualTo(expectedResponse);

        verify(aiTextFilterMapper, times(1)).fromProfileUpdate(requestDto);
        verify(aiApiClient, times(1)).filterText(textFilterRequestDto);
        verify(userCommandService, times(1))
                .updateUserProfile(targetUserId, currentUserId, requestDto);

        verifyNoMoreInteractions(aiTextFilterMapper, aiApiClient, userCommandService);
    }

    @Test
    @DisplayName("프로필 수정 (400 Bad Request): 닉네임에 금칙어가 있으면 INVALID_NICKNAME 예외")
    void updateUserProfile_invalidNickname() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 1L;

        UserProfileUpdateRequestDto requestDto =
                new UserProfileUpdateRequestDto(null, "금칙어닉네임", 24, "MALE", 20, List.of("SPORTS"));

        TextFilterRequestDto textFilterRequestDto = mock(TextFilterRequestDto.class);
        TextFilterResponseDto textFilterResponseDto = mock(TextFilterResponseDto.class);

        when(aiTextFilterMapper.fromProfileUpdate(requestDto)).thenReturn(textFilterRequestDto);
        when(aiApiClient.filterText(textFilterRequestDto)).thenReturn(textFilterResponseDto);
        when(textFilterResponseDto.isAllowed()).thenReturn(false);

        // when & then
        assertThatThrownBy(
                        () ->
                                userService.updateUserProfile(
                                        targetUserId, currentUserId, requestDto))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.INVALID_NICKNAME);

        verify(aiTextFilterMapper, times(1)).fromProfileUpdate(requestDto);
        verify(aiApiClient, times(1)).filterText(textFilterRequestDto);
        verify(userCommandService, never()).updateUserProfile(any(), any(), any());

        verifyNoMoreInteractions(aiTextFilterMapper, aiApiClient);
        verifyNoInteractions(userCommandService);
    }
}
