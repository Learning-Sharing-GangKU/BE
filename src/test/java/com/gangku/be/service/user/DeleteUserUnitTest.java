package com.gangku.be.service.user;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.gangku.be.domain.User;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.exception.CustomException;
import com.gangku.be.repository.ParticipationRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.UserService;
import com.gangku.be.support.UserLookup;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class DeleteUserUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private ParticipationRepository participationRepository;
    @Mock private UserLookup userLookup;

    @InjectMocks private UserService userService;

    @Test
    @DisplayName("회원 탈퇴 (204 No Content): 본인 요청이면 유저 삭제")
    void deleteUser_success() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 1L;

        User user = User.builder().id(targetUserId).participations(new ArrayList<>()).build();

        when(userLookup.findById(targetUserId)).thenReturn(user);
        when(participationRepository.findAllByUserWithGatheringAndHost(user))
                .thenReturn(Collections.emptyList());

        // when
        userService.deleteUser(targetUserId, currentUserId);

        // then
        verify(userLookup, times(1)).findById(targetUserId);
        verify(participationRepository, times(1)).findAllByUserWithGatheringAndHost(user);
        verify(participationRepository, times(1)).deleteAll(Collections.emptyList());
        verify(userRepository, times(1)).delete(user);

        verifyNoMoreInteractions(userLookup, userRepository, participationRepository);
    }

    @Test
    @DisplayName("회원 탈퇴 (404 Not Found): 대상 유저가 없으면 USER_NOT_FOUND 예외")
    void deleteUser_userNotFound() {
        // given
        Long targetUserId = 999L;
        Long currentUserId = 999L;

        when(userLookup.findById(targetUserId)).thenThrow(new CustomException(UserErrorCode.USER_NOT_FOUND));

        // when
        assertThatThrownBy(() -> userService.deleteUser(targetUserId, currentUserId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        // then
        verify(userLookup, times(1)).findById(targetUserId);
        verify(userRepository, never()).delete(any());
        verifyNoMoreInteractions(userLookup);
    }

    @Test
    @DisplayName("회원 탈퇴 (403 Forbidden): 본인이 아니면 NO_PERMISSION_TO_CANCEL_MEMBERSHIP 예외")
    void deleteUser_noPermission() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 2L;

        User user = User.builder().id(1L).build();

        when(userLookup.findById(targetUserId)).thenReturn(user);

        // when
        assertThatThrownBy(() -> userService.deleteUser(targetUserId, currentUserId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.NO_PERMISSION_TO_ACCESS_OTHER_USER_INFORMATION);

        // then
        verify(userLookup, times(1)).findById(targetUserId);
        verify(userRepository, never()).delete(any());
        verifyNoMoreInteractions(userLookup);
    }
}
