package com.gangku.be.dto.participation;

import com.gangku.be.model.PageMeta;
import com.gangku.be.model.ParticipantsPreview;
import com.gangku.be.model.ParticipantsPreviewItem;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class ParticipantsPreviewResponseDto {

    private final List<ParticipantsPreviewItem> data;
    private final PageMeta meta;

    public static ParticipantsPreviewResponseDto from(ParticipantsPreview preview) {
        return ParticipantsPreviewResponseDto.builder()
                .data(preview.data())
                .meta(preview.meta())
                .build();
    }
}
