package com.gangku.be.dto.gathering.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GatheringIntroRequestDto {

    @NotBlank
    @Size(min = 1, max = 30, message = "제목은 1자 이상 30자 이하여야 합니다.")
    private String title;

    @Min(value = 1)
    @Max(value = 100)
    private String category;

    @NotNull
    private LocalDateTime date;

    @NotBlank
    @Size(min = 1, max = 30, message = "장소는 1자 이상 30자 이하여야 합니다.")
    private String location;

    @NotEmpty
    private List<String> keywords;
}
