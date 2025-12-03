package com.gangku.be.dto.gathering.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GatheringRecommendAiRequest {

    // ====== 내부에서만 쓰는 Gathering 클래스 ======
    /*
    이걸 근데 내부적으로 논의해봐야되는게, 백에서 gathering DB entity 에서 해당항목을 다 넘겨줄 것인가
    아니면 그냥 방 뭉탱이를 다 AI로 넘겨주면 AI 에서 전처리 해서 사용할 것인가 근데 아까 이렇게 보내기로 하긴 했으니깐 이렇게는 쓸게요

    저기서 hostAge 를 DB gatherings entity 에 host 의 userId가 있던데 또 거기까지 가서 AI로 보내야되기때문에 번거로울 수도 있음
    -> 좀 많이 번거로우면 그냥 없애는 방향으로 해도 되긴함

    근데 이게 모든 방을 list 로 넘겨줘야돼서 좀 그렇긴 함..
     */
    @Data
    public static class Gathering {

        @NotNull
        @Positive
        private Long gatheringId;

        @NotNull
        private String category;

        @Min(0)
        private Integer hostAge;

        @NotNull
        private Integer capacity;

        @NotNull
        private Integer participantCount;

        @NotNull
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private LocalDateTime createdAt;
    }

    // 이거 어떻게 뭐 pathparam으로 받는지 어떻게 받는지 모르겠는데 암튼 이렇게 둘게요
    // 그리고 이게

    //Nullable
    private int userId;

    @Min(value = 14)
    @Max(value = 100)
    //Nullable
    private Integer age;

    @Size(max = 3)
    //Nullable
    private List<String> preferredCategories;

    @NotNull
    @Size(min = 1)
    @Valid
    private List<Gathering> gatherings;
}


