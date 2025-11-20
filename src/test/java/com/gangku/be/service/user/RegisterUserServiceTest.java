package com.gangku.be.service.user;

import com.gangku.be.domain.Category;
import com.gangku.be.domain.PreferredCategory;
import com.gangku.be.domain.User;
import com.gangku.be.dto.user.SignUpRequestDto;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.repository.CategoryRepository;
import com.gangku.be.repository.PreferredCategoryRepository;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.service.UserService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PreferredCategoryRepository preferredCategoryRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    /**
     * User.create() 에서 profileImage.* 를 사용하므로
     * 정상/경계 플로우 테스트에서 반드시 호출해줘야 한다.
     */
    private void stubProfileImage(SignUpRequestDto dto) {
        SignUpRequestDto.ProfileImage profileImage = mock(SignUpRequestDto.ProfileImage.class);
        when(dto.getProfileImage()).thenReturn(profileImage);

        // User.create(...) 내부에서 사용하는 필드만 최소한으로 세팅
        when(profileImage.getBucket()).thenReturn("test-bucket");
        when(profileImage.getKey()).thenReturn("profile/key.jpg");
    }

    // =========================================================
    // 1. 정상 케이스
    // =========================================================

    @Test
    @DisplayName("유효한 입력 + 선호 카테고리 2개 → User 생성 & 2개의 PreferredCategory 저장")
    void registerUser_withValidInput_createsUserAndPreferredCategories() {
        // given
        SignUpRequestDto dto = mock(SignUpRequestDto.class);
        String email = "user@test.com";
        String password = "Abcd1234";
        String nickname = "user1";
        List<String> preferred = List.of("Backend", "Frontend");

        when(dto.getEmail()).thenReturn(email);
        when(dto.getPassword()).thenReturn(password);
        when(dto.getNickname()).thenReturn(nickname);
        when(dto.getPreferredCategories()).thenReturn(preferred);

        stubProfileImage(dto);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.existsByNickname(nickname)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPw");

        // Category 엔티티 2개 준비
        Category backend = new Category();
        backend.setName("Backend");
        Category frontend = new Category();
        frontend.setName("Frontend");

        when(categoryRepository.findByNameIn(anyList()))
                .thenReturn(List.of(backend, frontend));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        User result = userService.registerUser(dto);

        // then
        assertNotNull(result, "User 결과가 null이면 안 된다.");

        verify(categoryRepository).findByNameIn(argThat(names ->
                names.size() == 2 &&
                        names.contains("Backend") &&
                        names.contains("Frontend")
        ));

        verify(preferredCategoryRepository).saveAll(argThat((List<PreferredCategory> list) ->
                list.size() == 2
        ));

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("비밀번호가 규칙을 딱 만족하는 최소 길이(8자)인 경우 → 정상 가입")
    void registerUser_withMinValidPasswordLength_registersSuccessfully() {
        // given
        SignUpRequestDto dto = mock(SignUpRequestDto.class);
        String email = "min@test.com";
        String password = "Abcdef12"; // 대문자/소문자/숫자 포함 8자
        String nickname = "minUser";

        when(dto.getEmail()).thenReturn(email);
        when(dto.getPassword()).thenReturn(password);
        when(dto.getNickname()).thenReturn(nickname);
        when(dto.getPreferredCategories()).thenReturn(Collections.emptyList());

        stubProfileImage(dto);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.existsByNickname(nickname)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPw");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        User result = userService.registerUser(dto);

        // then
        assertNotNull(result);
        verify(preferredCategoryRepository, never()).saveAll(anyList());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("선호 카테고리에 중복된 이름이 있을 때 → distinct 처리 후 중복 없이 저장")
    void registerUser_withDuplicatedPreferredCategories_savesDistinctPreferredCategories() {
        // given
        SignUpRequestDto dto = mock(SignUpRequestDto.class);
        String email = "dup@test.com";
        String password = "Abcd1234";
        String nickname = "dupUser";
        List<String> preferred = Arrays.asList("Backend", "Backend", "Frontend");

        when(dto.getEmail()).thenReturn(email);
        when(dto.getPassword()).thenReturn(password);
        when(dto.getNickname()).thenReturn(nickname);
        when(dto.getPreferredCategories()).thenReturn(preferred);

        stubProfileImage(dto);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.existsByNickname(nickname)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPw");

        Category backend = new Category();
        backend.setName("Backend");
        Category frontend = new Category();
        frontend.setName("Frontend");

        when(categoryRepository.findByNameIn(anyList()))
                .thenReturn(List.of(backend, frontend));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        User result = userService.registerUser(dto);

        // then
        assertNotNull(result);

        verify(categoryRepository).findByNameIn(argThat(names ->
                names.size() == 2 &&
                        names.contains("Backend") &&
                        names.contains("Frontend")
        ));

        verify(preferredCategoryRepository).saveAll(argThat((List<PreferredCategory> list) ->
                list.size() == 2
        ));
    }

    @Test
    @DisplayName("선호 카테고리 중 일부만 실제 존재하는 경우 → 존재하는 카테고리만 매핑")
    void registerUser_withPartiallyExistingPreferredCategories_savesOnlyExistingOnes() {
        // given
        SignUpRequestDto dto = mock(SignUpRequestDto.class);
        String email = "partial@test.com";
        String password = "Abcd1234";
        String nickname = "partialUser";
        List<String> preferred = List.of("Backend", "Unknown");

        when(dto.getEmail()).thenReturn(email);
        when(dto.getPassword()).thenReturn(password);
        when(dto.getNickname()).thenReturn(nickname);
        when(dto.getPreferredCategories()).thenReturn(preferred);

        stubProfileImage(dto);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.existsByNickname(nickname)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPw");

        Category backend = new Category();
        backend.setName("Backend");

        when(categoryRepository.findByNameIn(anyList()))
                .thenReturn(List.of(backend));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        User result = userService.registerUser(dto);

        // then
        assertNotNull(result);

        verify(preferredCategoryRepository).saveAll(argThat((List<PreferredCategory> list) ->
                list.size() == 1
        ));
    }

    @Test
    @DisplayName("선호 카테고리가 빈 리스트일 때 → 선호 카테고리 없이 유저만 생성")
    void registerUser_withEmptyPreferredCategories_createsUserWithoutPreferences() {
        // given
        SignUpRequestDto dto = mock(SignUpRequestDto.class);
        String email = "empty@test.com";
        String password = "Abcd1234";
        String nickname = "emptyUser";

        when(dto.getEmail()).thenReturn(email);
        when(dto.getPassword()).thenReturn(password);
        when(dto.getNickname()).thenReturn(nickname);
        when(dto.getPreferredCategories()).thenReturn(Collections.emptyList());

        stubProfileImage(dto);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.existsByNickname(nickname)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPw");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        User result = userService.registerUser(dto);

        // then
        assertNotNull(result);
        verify(categoryRepository, never()).findByNameIn(anyList());
        verify(preferredCategoryRepository, never()).saveAll(anyList());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("선호 카테고리 이름에 공백/대소문자 차이가 있는 경우 → Repository가 반환한 것만 매핑")
    void registerUser_withPreferredCategoryNameVariants_handlesAccordingToRepositoryMatching() {
        // given
        SignUpRequestDto dto = mock(SignUpRequestDto.class);
        String email = "variant@test.com";
        String password = "Abcd1234";
        String nickname = "variantUser";
        List<String> preferred = Arrays.asList("backend", " Backend ", "FRONTEND");

        when(dto.getEmail()).thenReturn(email);
        when(dto.getPassword()).thenReturn(password);
        when(dto.getNickname()).thenReturn(nickname);
        when(dto.getPreferredCategories()).thenReturn(preferred);

        stubProfileImage(dto);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.existsByNickname(nickname)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPw");

        Category backend = new Category();
        backend.setName("Backend");

        when(categoryRepository.findByNameIn(anyList()))
                .thenReturn(List.of(backend));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        User result = userService.registerUser(dto);

        // then
        assertNotNull(result);

        verify(preferredCategoryRepository).saveAll(argThat((List<PreferredCategory> list) ->
                list.size() == 1
        ));
    }

    // =========================================================
    // 2. 예외 케이스
    // =========================================================

    @Test
    @DisplayName("이메일 형식이 잘못된 경우 → INVALID_EMAIL_FORMAT")
    void registerUser_withInvalidEmailFormat_throwsInvalidEmailFormat() {
        // given
        SignUpRequestDto dto = mock(SignUpRequestDto.class);

        // 이 테스트에서는 email만 사용되고, password/nickname 은 호출되지 않음
        when(dto.getEmail()).thenReturn("not-an-email");

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> userService.registerUser(dto));

        // then
        assertEquals(UserErrorCode.INVALID_EMAIL_FORMAT, ex.getErrorCode());
        verifyNoInteractions(userRepository, categoryRepository, preferredCategoryRepository, passwordEncoder);
    }

    @Test
    @DisplayName("비밀번호가 규칙을 만족하지 못하는 경우 → PASSWORD_TOO_WEAK")
    void registerUser_withWeakPassword_throwsPasswordTooWeak() {
        // given
        SignUpRequestDto dto = mock(SignUpRequestDto.class);

        // 이메일 형식은 통과, 비밀번호 규칙에서 막힌다
        when(dto.getEmail()).thenReturn("user@test.com");
        when(dto.getPassword()).thenReturn("abcdef12");   // 소문자+숫자, 대문자 없음 → 규칙 위반

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> userService.registerUser(dto));

        // then
        assertEquals(UserErrorCode.PASSWORD_TOO_WEAK, ex.getErrorCode());
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).existsByNickname(anyString());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("이미 존재하는 이메일인 경우 → EMAIL_ALREADY_EXISTS")
    void registerUser_withExistingEmail_throwsEmailAlreadyExists() {
        // given
        SignUpRequestDto dto = mock(SignUpRequestDto.class);
        String email = "dup@test.com";

        when(dto.getEmail()).thenReturn(email);
        when(dto.getPassword()).thenReturn("Abcd1234"); // 이메일/비밀번호는 통과 후 이메일 중복 단계에서 막힘

        when(userRepository.existsByEmail(email)).thenReturn(true);

        // when
        CustomException ex = assertThrows(CustomException.class,
                () -> userService.registerUser(dto));

        // then
        assertEquals(UserErrorCode.EMAIL_ALREADY_EXISTS, ex.getErrorCode());
        verify(userRepository).existsByEmail(email);
        verify(userRepository, never()).existsByNickname(anyString());
        verify(passwordEncoder, never()).encode(anyString());
    }

    // =========================================================
    // 3. 경계 / 구현상 버그 확인 케이스
    // =========================================================

    @Test
    @DisplayName("선호 카테고리가 null 인 경우 → 현재 구현 기준으로는 NullPointerException 발생")
    void registerUser_withNullPreferredCategories_throwsNullPointer() {
        // given
        SignUpRequestDto dto = mock(SignUpRequestDto.class);
        String email = "nullpref@test.com";
        String password = "Abcd1234";
        String nickname = "nullPrefUser";

        when(dto.getEmail()).thenReturn(email);
        when(dto.getPassword()).thenReturn(password);
        when(dto.getNickname()).thenReturn(nickname);
        when(dto.getPreferredCategories()).thenReturn(null); // ★ null

        stubProfileImage(dto);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.existsByNickname(nickname)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPw");

        // when & then
        assertThrows(NullPointerException.class,
                () -> userService.registerUser(dto));

        verify(preferredCategoryRepository, never()).saveAll(anyList());
    }
}
