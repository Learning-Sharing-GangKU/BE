package com.gangku.be.dto.gathering.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gangku.be.constant.category.CategoryList;
import com.gangku.be.exception.annotation.ValidEnum;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GatheringCreateRequestDto {

    @NotBlank
    @Size(min = 1, max = 30)
    private String title;

    private String gatheringImageObjectKey;

    @NotBlank
    @ValidEnum(enumClass = CategoryList.class, ignoreCase = true)
    private String category;

    @Min(value = 1)
    @Max(value = 100)
    private Integer capacity;

    @NotNull
    @Future
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss",
            timezone = "Asia/Seoul")
    private LocalDateTime date;

    @NotBlank
    @Size(min = 1, max = 30)
    private String location;

    @NotBlank
    @Pattern(regexp = "^https://.*")
    private String openChatUrl;

    @NotBlank
    @Size(max = 800)
    private String description;
}
