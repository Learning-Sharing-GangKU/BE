package com.gangku.be.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.gangku.be.domain.User;
import com.gangku.be.dto.ai.request.TextFilterRequestDto;
import com.gangku.be.dto.ai.response.TextFilterResponseDto;
import com.gangku.be.dto.user.SignUpRequestDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.AuthErrorCode;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.external.ai.AiApiClient;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.PreferredCategoryRepository;
import com.gangku.be.repository.ReviewRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.UserService;
import com.gangku.be.util.ai.AiTextFilterMapper;
import com.gangku.be.util.object.FileUrlResolver;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class RegisterUserUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private PreferredCategoryRepository preferredCategoryRepository;
    @Mock private FileUrlResolver fileUrlResolver;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ReviewRepository reviewRepository;
    @Mock private AiApiClient aiApiClient;
    @Mock private AiTextFilterMapper aiTextFilterMapper;
    @Mock private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks private UserService userService;

    @Test
    @DisplayName("회원가입 성공: 이메일 인증 완료, 이메일/닉네임 중복 없음, 금칙어 없음이면 회원가입 성공")
    void registerUser_success() {
        // given
        String sessionId = "session-123";
        String sessionKey = "auth:signup:session:" + sessionId;

        SignUpRequestDto requestDto =
                new SignUpRequestDto(
                        "test@example.com",
                        "plain-password",
                        "정상닉네임",
                        24,
                        "MALE",
                        20,
                        null,
                        null);

        TextFilterRequestDto textFilterRequestDto = mock(TextFilterRequestDto.class);
        TextFilterResponseDto textFilterResponseDto = mock(TextFilterResponseDto.class);

        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(sessionKey))
                .thenReturn(Map.of("verified", "1", "email", "test@example.com"));

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByNickname("정상닉네임")).thenReturn(false);

        when(aiTextFilterMapper.fromSignUp(requestDto)).thenReturn(textFilterRequestDto);
        when(aiApiClient.filterText(textFilterRequestDto)).thenReturn(textFilterResponseDto);
        when(textFilterResponseDto.isAllowed()).thenReturn(true);

        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");

        // when
        User savedUser = userService.registerUser(requestDto, sessionId);

        // then
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getNickname()).isEqualTo("정상닉네임");
        assertThat(savedUser.getAge()).isEqualTo(24);
        assertThat(savedUser.getGender()).isEqualTo("MALE");
        assertThat(savedUser.getEnrollNumber()).isEqualTo(20);

        verify(stringRedisTemplate, times(1)).opsForHash();
        verify(hashOperations, times(1)).entries(sessionKey);
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        verify(userRepository, times(1)).existsByNickname("정상닉네임");
        verify(aiTextFilterMapper, times(1)).fromSignUp(requestDto);
        verify(aiApiClient, times(1)).filterText(textFilterRequestDto);
        verify(passwordEncoder, times(1)).encode("plain-password");
        verify(userRepository, times(1)).save(any(User.class));
        verify(stringRedisTemplate, times(1)).delete(sessionKey);

        verifyNoInteractions(categoryRepository, preferredCategoryRepository, fileUrlResolver, reviewRepository);
        verifyNoMoreInteractions(
                userRepository,
                stringRedisTemplate,
                passwordEncoder,
                aiTextFilterMapper,
                aiApiClient,
                hashOperations);
    }

    @Test
    @DisplayName("회원가입 실패(403): 이메일 인증이 완료되지 않으면 EMAIL_NOT_VERIFIED 예외")
    void registerUser_emailNotVerified() {
        // given
        String sessionId = "session-123";
        String sessionKey = "auth:signup:session:" + sessionId;

        SignUpRequestDto requestDto =
                new SignUpRequestDto(
                        "test@example.com",
                        "plain-password",
                        "정상닉네임",
                        24,
                        "MALE",
                        20,
                        null,
                        null);

        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(sessionKey)).thenReturn(Map.of());

        // when & then
        assertThatThrownBy(() -> userService.registerUser(requestDto, sessionId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.EMAIL_NOT_VERIFIED);

        verify(stringRedisTemplate, times(1)).opsForHash();
        verify(hashOperations, times(1)).entries(sessionKey);

        verifyNoInteractions(
                userRepository,
                categoryRepository,
                preferredCategoryRepository,
                fileUrlResolver,
                passwordEncoder,
                reviewRepository,
                aiApiClient,
                aiTextFilterMapper);
        verifyNoMoreInteractions(stringRedisTemplate, hashOperations);
    }

    @Test
    @DisplayName("회원가입 실패(409): 이메일이 이미 존재하면 EMAIL_ALREADY_EXISTS 예외")
    void registerUser_emailConflict() {
        // given
        String sessionId = "session-123";
        String sessionKey = "auth:signup:session:" + sessionId;

        SignUpRequestDto requestDto =
                new SignUpRequestDto(
                        "test@example.com",
                        "plain-password",
                        "정상닉네임",
                        24,
                        "MALE",
                        20,
                        null,
                        null);

        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(sessionKey))
                .thenReturn(Map.of("verified", "1", "email", "test@example.com"));
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.registerUser(requestDto, sessionId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.EMAIL_ALREADY_EXISTS);

        verify(stringRedisTemplate, times(1)).opsForHash();
        verify(hashOperations, times(1)).entries(sessionKey);
        verify(userRepository, times(1)).existsByEmail("test@example.com");

        verify(userRepository, never()).existsByNickname(anyString());
        verify(userRepository, never()).save(any());
        verify(stringRedisTemplate, never()).delete(anyString());

        verifyNoInteractions(
                categoryRepository,
                preferredCategoryRepository,
                fileUrlResolver,
                passwordEncoder,
                reviewRepository,
                aiApiClient,
                aiTextFilterMapper);
        verifyNoMoreInteractions(userRepository, stringRedisTemplate, hashOperations);
    }

    @Test
    @DisplayName("회원가입 실패(409): 닉네임이 이미 존재하면 NICKNAME_ALREADY_EXISTS 예외")
    void registerUser_nicknameConflict() {
        // given
        String sessionId = "session-123";
        String sessionKey = "auth:signup:session:" + sessionId;

        SignUpRequestDto requestDto =
                new SignUpRequestDto(
                        "test@example.com",
                        "plain-password",
                        "중복닉네임",
                        24,
                        "MALE",
                        20,
                        null,
                        null);

        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(sessionKey))
                .thenReturn(Map.of("verified", "1", "email", "test@example.com"));
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByNickname("중복닉네임")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.registerUser(requestDto, sessionId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.NICKNAME_ALREADY_EXISTS);

        verify(stringRedisTemplate, times(1)).opsForHash();
        verify(hashOperations, times(1)).entries(sessionKey);
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        verify(userRepository, times(1)).existsByNickname("중복닉네임");

        verify(userRepository, never()).save(any());
        verify(stringRedisTemplate, never()).delete(anyString());

        verifyNoInteractions(
                categoryRepository,
                preferredCategoryRepository,
                fileUrlResolver,
                passwordEncoder,
                reviewRepository,
                aiApiClient,
                aiTextFilterMapper);
        verifyNoMoreInteractions(userRepository, stringRedisTemplate, hashOperations);
    }

    @Test
    @DisplayName("회원가입 실패(400): 닉네임에 금칙어가 포함되면 INVALID_NICKNAME 예외")
    void registerUser_invalidNickname() {
        // given
        String sessionId = "session-123";
        String sessionKey = "auth:signup:session:" + sessionId;

        SignUpRequestDto requestDto =
                new SignUpRequestDto(
                        "test@example.com",
                        "plain-password",
                        "금칙어닉네임",
                        24,
                        "MALE",
                        20,
                        null,
                        null);

        TextFilterRequestDto textFilterRequestDto = mock(TextFilterRequestDto.class);
        TextFilterResponseDto textFilterResponseDto = mock(TextFilterResponseDto.class);

        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(sessionKey))
                .thenReturn(Map.of("verified", "1", "email", "test@example.com"));
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByNickname("금칙어닉네임")).thenReturn(false);

        when(aiTextFilterMapper.fromSignUp(requestDto)).thenReturn(textFilterRequestDto);
        when(aiApiClient.filterText(textFilterRequestDto)).thenReturn(textFilterResponseDto);
        when(textFilterResponseDto.isAllowed()).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.registerUser(requestDto, sessionId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.INVALID_NICKNAME);

        verify(stringRedisTemplate, times(1)).opsForHash();
        verify(hashOperations, times(1)).entries(sessionKey);
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        verify(userRepository, times(1)).existsByNickname("금칙어닉네임");
        verify(aiTextFilterMapper, times(1)).fromSignUp(requestDto);
        verify(aiApiClient, times(1)).filterText(textFilterRequestDto);

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
        verify(stringRedisTemplate, never()).delete(anyString());

        verifyNoInteractions(categoryRepository, preferredCategoryRepository, fileUrlResolver, reviewRepository);
        verifyNoMoreInteractions(
                userRepository,
                stringRedisTemplate,
                aiTextFilterMapper,
                aiApiClient,
                passwordEncoder,
                hashOperations);
    }
}