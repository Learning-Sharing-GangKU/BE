package com.gangku.be.util.ai;

import com.gangku.be.dto.ai.request.TextFilterRequestDto;
import com.gangku.be.dto.gathering.request.GatheringCreateRequestDto;
import com.gangku.be.dto.gathering.request.GatheringUpdateRequestDto;
import com.gangku.be.dto.review.ReviewCreateRequestDto;
import com.gangku.be.dto.user.SignUpRequestDto;
import com.gangku.be.dto.user.UserProfileUpdateRequestDto;
import org.springframework.stereotype.Component;

@Component
public class AiTextFilterMapper {

    private static final String FIELD_SEPARATOR = " ||| ";

    public TextFilterRequestDto fromSignUp(SignUpRequestDto signUpRequestDto) {
        return new TextFilterRequestDto(signUpRequestDto.getNickname().trim());
    }

    public TextFilterRequestDto fromProfileUpdate(UserProfileUpdateRequestDto userProfileUpdateRequestDto) {
        return new TextFilterRequestDto(userProfileUpdateRequestDto.getNickname().trim());
    }

    public TextFilterRequestDto fromReviewCreate(ReviewCreateRequestDto reviewCreateRequestDto) {
        return new TextFilterRequestDto(reviewCreateRequestDto.getComment().trim());
    }

    public TextFilterRequestDto fromGatheringCreate(GatheringCreateRequestDto  gatheringCreateRequestDto) {
        return new TextFilterRequestDto(joinText(gatheringCreateRequestDto.getTitle().trim(),
                gatheringCreateRequestDto.getDescription()).trim());
    }

    public TextFilterRequestDto fromGatheringUpdate(GatheringUpdateRequestDto gatheringUpdateRequestDto) {
        return new TextFilterRequestDto(joinText(gatheringUpdateRequestDto.getTitle().trim(),
                gatheringUpdateRequestDto.getDescription()).trim());
    }

    private String joinText(String title, String description) {

        if (title.isBlank()) {
            return description;
        }
        if (description.isBlank()) {
            return title;
        }

        return title + FIELD_SEPARATOR + description;
    }
}
