package com.gangku.be.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.gangku.be.domain.User;
import com.gangku.be.dto.user.UpdateReviewSettingResponseDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.PreferredCategoryRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class UpdateReviewSettingUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private PreferredCategoryRepository preferredCategoryRepository;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    @Test
    @DisplayName("리뷰 설정 변경 (200 OK): 본인 요청이면 reviewsPublic 값 변경 후 저장")
    void updateReviewSetting_success() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 1L;
        Boolean reviewSetting = true;

        User user = User.builder().id(targetUserId).build();

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        // when
        UpdateReviewSettingResponseDto response =
                userService.updateReviewSetting(targetUserId, currentUserId, reviewSetting);

        // then
        assertThat(response).isNotNull();

        verify(userRepository, times(1)).findById(targetUserId);
        verify(userRepository, times(1)).save(user);
        verifyNoMoreInteractions(userRepository);

        verifyNoInteractions(categoryRepository, preferredCategoryRepository, stringRedisTemplate, passwordEncoder);
    }

    @Test
    @DisplayName("리뷰 설정 변경 (404 Not Found): 대상 유저가 없으면 USER_NOT_FOUND 예외")
    void updateReviewSetting_userNotFound() {
        // given
        Long targetUserId = 999L;
        Long currentUserId = 999L;
        Boolean reviewSetting = false;

        when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

        // when
        assertThatThrownBy(() -> userService.updateReviewSetting(targetUserId, currentUserId, reviewSetting))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        // then
        verify(userRepository, times(1)).findById(targetUserId);
        verify(userRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository);

        verifyNoInteractions(categoryRepository, preferredCategoryRepository, stringRedisTemplate, passwordEncoder);
    }

    @Test
    @DisplayName("리뷰 설정 변경 (403 Forbidden): 본인이 아니면 NO_PERMISSION_TO_ACCESS_OTHER_USER_INFORMATION 예외")
    void updateReviewSetting_noPermission() {
        // given
        Long targetUserId = 1L;
        Long currentUserId = 2L;
        Boolean reviewSetting = true;

        User user = User.builder().id(targetUserId).build();

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user));

        // when
        assertThatThrownBy(() -> userService.updateReviewSetting(targetUserId, currentUserId, reviewSetting))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.NO_PERMISSION_TO_ACCESS_OTHER_USER_INFORMATION);

        // then
        verify(userRepository, times(1)).findById(targetUserId);
        verify(userRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository);

        verifyNoInteractions(categoryRepository, preferredCategoryRepository, stringRedisTemplate, passwordEncoder);
    }
}