package com.gangku.be.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.User;
import com.gangku.be.dto.user.SignUpRequestDto;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.PreferredCategoryRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.command.UserCommandService;
import com.gangku.be.util.object.FileUrlResolver;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class RegisterUserCommandUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private PreferredCategoryRepository preferredCategoryRepository;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private FileUrlResolver fileUrlResolver;

    @InjectMocks private UserCommandService userCommandService;

    @Test
    @DisplayName("회원가입 저장 성공: 선호 카테고리 없는 경우")
    void saveUser_success_withoutPreferredCategories() {
        // given
        String sessionId = "session-123";
        String sessionKey = "auth:signup:session:" + sessionId;

        SignUpRequestDto requestDto =
                new SignUpRequestDto(
                        "test@example.com", "plain-password", "정상닉네임", 24, "MALE", 20, null, null);

        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        User result = userCommandService.saveUser(requestDto, sessionId);

        // then
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getPassword()).isEqualTo("encoded-password");
        assertThat(result.getNickname()).isEqualTo("정상닉네임");
        assertThat(result.getAge()).isEqualTo(24);
        assertThat(result.getGender()).isEqualTo("MALE");
        assertThat(result.getEnrollNumber()).isEqualTo(20);

        verify(passwordEncoder, times(1)).encode("plain-password");
        verify(userRepository, times(1)).save(any(User.class));
        verify(stringRedisTemplate, times(1)).delete(sessionKey);

        verifyNoInteractions(categoryRepository, preferredCategoryRepository);
        verifyNoMoreInteractions(passwordEncoder, userRepository, stringRedisTemplate);
    }

    @Test
    @DisplayName("회원가입 저장 성공: 선호 카테고리 있는 경우")
    void saveUser_success_withPreferredCategories() {
        // given
        String sessionId = "session-123";
        String sessionKey = "auth:signup:session:" + sessionId;

        SignUpRequestDto requestDto =
                new SignUpRequestDto(
                        "test@example.com", "plain-password", "정상닉네임", 24, "MALE", 20, null,
                        List.of("SPORTS", "MUSIC"));

        Category sports = mock(Category.class);
        Category music = mock(Category.class);

        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryRepository.findByNameIn(List.of("SPORTS", "MUSIC")))
                .thenReturn(List.of(sports, music));

        // when
        User result = userCommandService.saveUser(requestDto, sessionId);

        // then
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getPassword()).isEqualTo("encoded-password");

        verify(passwordEncoder, times(1)).encode("plain-password");
        verify(userRepository, times(1)).save(any(User.class));
        verify(stringRedisTemplate, times(1)).delete(sessionKey);
        verify(categoryRepository, times(1)).findByNameIn(List.of("SPORTS", "MUSIC"));
        verify(preferredCategoryRepository, times(1)).saveAll(anyList());

        verifyNoMoreInteractions(
                passwordEncoder, userRepository, stringRedisTemplate,
                categoryRepository, preferredCategoryRepository);
    }
}