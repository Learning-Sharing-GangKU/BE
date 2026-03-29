package com.gangku.be.dto.gathering.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GatheringUpdateRequestDto {

    @Size(min = 1, max = 30)
    private String title;

    private String gatheringImageObjectKey;

    private String category;

    @Min(value = 1)
    @Max(value = 100)
    private Integer capacity;

    @Future
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ssX",
            timezone = "Asia/Seoul")
    private LocalDateTime date;

    @Size(min = 1, max = 30)
    private String location;

    @Pattern(regexp = "^https://.*")
    private String openChatUrl;

    @Size(max = 800)
    private String description;
}
