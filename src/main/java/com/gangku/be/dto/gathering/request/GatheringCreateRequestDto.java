package com.gangku.be.dto.gathering.request;

import com.gangku.be.model.ImageObject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GatheringCreateRequestDto {

    @NotBlank
    @Size(min = 1, max = 30, message = "제목은 1자 이상 30자 이하여야 합니다.")
    private String title;

    @Valid
    private ImageObject gatheringImage;

    @NotBlank
    private String category;

    @Min(value = 1)
    @Max(value = 100)
    private int capacity;

    @NotNull
    private LocalDateTime date;

    @NotBlank
    @Size(min = 1, max = 30, message = "장소는 1자 이상 30자 이하여야 합니다.")
    private String location;

    @NotBlank
    @Pattern(regexp = "^https://.*", message = "오픈채팅 URL은 https로 시작해야 합니다.")
    private String openChatUrl;

    @NotBlank
    @Size(max = 800, message = "소개글은 최대 800자까지 작성 가능합니다.")
    private String description;
}