package com.gangku.be.user;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.gangku.be.domain.User;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private UserService userService;

    @Test
    @DisplayName("회원 탈퇴 (204 No Content): 본인 요청이면 유저 삭제")
    void deleteUser_success() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 1L;

        User user = User.builder().id(targetUserId).build();

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user));

        // when
        userService.deleteUser(targetUserId, currentUserId);

        // then
        verify(userRepository).delete(user);
        verify(userRepository, times(1)).findById(targetUserId);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("회원 탈퇴 (404 Not Found): 대상 유저가 없으면 USER_NOT_FOUND 예외")
    void deleteUser_userNotFound() {
        // given
        Long targetUserId = 999L;
        Long currentUserId = 999L;

        when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.deleteUser(targetUserId, currentUserId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        verify(userRepository, times(1)).findById(targetUserId);
        verify(userRepository, never()).delete(any());
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("회원 탈퇴 (403 Forbidden): 본인이 아니면 NO_PERMISSION_TO_CANCEL_MEMBERSHIP 예외")
    void deleteUser_noPermission() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 2L;

        User user = User.builder().id(1L).build();

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.deleteUser(targetUserId, currentUserId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.NO_PERMISSION_TO_CANCEL_MEMBERSHIP);

        verify(userRepository, times(1)).findById(targetUserId);
        verify(userRepository, never()).delete(any());
        verifyNoMoreInteractions(userRepository);
    }
}
