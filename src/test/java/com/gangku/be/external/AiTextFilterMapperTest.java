package com.gangku.be.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.gangku.be.dto.ai.request.TextFilterRequestDto;
import com.gangku.be.dto.gathering.request.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.request.GatheringUpdateRequestDto;
import com.gangku.be.dto.user.SignUpRequestDto;
import com.gangku.be.dto.user.UserProfileUpdateRequestDto;
import com.gangku.be.util.ai.AiTextFilterMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class AiTextFilterMapperTest {

    private final AiTextFilterMapper mapper = new AiTextFilterMapper();

    @Test
    @DisplayName("모임 생성 text filter 요청 생성: title과 description을 구분자 '|||'로 합친다")
    void fromGatheringCreate() {
        GatheringCreateRequestDto requestDto =
                new GatheringCreateRequestDto(
                        "알고리즘 스터디",
                        null,
                        "study",
                        10,
                        LocalDateTime.of(2025, 10, 1, 10, 0),
                        "공학관 301",
                        "https://open.kakao.com/o/abc",
                        "기초부터 공부합니다.");

        TextFilterRequestDto result = mapper.fromGatheringCreate(requestDto);

        assertThat(result.getText()).isEqualTo("알고리즘 스터디|||기초부터 공부합니다.");
    }

    @Test
    @DisplayName("모임 수정 text filter 요청 생성: title과 description을 구분자 '|||'로 합친다")
    void fromGatheringUpdate() {
        GatheringUpdateRequestDto requestDto =
                new GatheringUpdateRequestDto(
                        "제목 수정",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "설명 수정");

        TextFilterRequestDto result = mapper.fromGatheringUpdate(requestDto);

        assertThat(result.getText()).isEqualTo("제목 수정|||설명 수정");
    }

    @Test
    @DisplayName("회원가입 text filter 요청 생성: nickname만 text로 넣는다")
    void fromSignUp() {
        SignUpRequestDto requestDto =
                new SignUpRequestDto(
                        "test@example.com",
                        "pw",
                        "정상닉네임",
                        24,
                        "MALE",
                        20,
                        null,
                        List.of("SPORTS"));

        TextFilterRequestDto result = mapper.fromSignUp(requestDto);

        assertThat(result.getText()).isEqualTo("정상닉네임");
    }

    @Test
    @DisplayName("프로필 수정 text filter 요청 생성: nickname만 text로 넣는다")
    void fromProfileUpdate() {
        UserProfileUpdateRequestDto requestDto =
                new UserProfileUpdateRequestDto(
                        null,
                        "새닉네임",
                        24,
                        "MALE",
                        20,
                        null);

        TextFilterRequestDto result = mapper.fromProfileUpdate(requestDto);

        assertThat(result.getText()).isEqualTo("새닉네임");
    }
}