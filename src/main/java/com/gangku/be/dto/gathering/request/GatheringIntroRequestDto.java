package com.gangku.be.dto.gathering.request;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @Size(min = 1, max = 30)
    private String title;

    @Min(value = 1)
    @Max(value = 100)
    private String category;

    @NotNull
    @JsonFormat(shape =  JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "Asia/Seoul")
    private LocalDateTime date;

    @NotBlank
    @Size(min = 1, max = 30)
    private String location;

    @NotEmpty
    private List<String> keywords;
}
