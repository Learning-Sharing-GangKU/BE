package com.gangku.be.model;

import com.gangku.be.domain.Participation;
import java.util.List;
import org.springframework.data.domain.Page;

public record ParticipantsPreview(
        List<ParticipantsPreviewItem> data,
        PageMeta meta
) {
    public static ParticipantsPreview from(
            Page<Participation> participationPage,
            int pageNumber,
            int size,
            String sortedBy
    ) {
        List<ParticipantsPreviewItem> items = participationPage.getContent().stream()
                .map(ParticipantsPreviewItem::from)
                .toList();

        PageMeta meta = PageMeta.from(
                participationPage,
                pageNumber,
                size,
                sortedBy
        );

        return new ParticipantsPreview(items, meta);
    }
}