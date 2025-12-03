package com.gangku.be.model;

import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record ParticipantsPreview(
        List<ParticipantsPreviewItem> data,
        PageMeta meta
) {
    public static ParticipantsPreview from(
            Page<Participation> participationPage,
            int pageNumber,
            int size,
            String sortedBy,
            Function<User, String> imageUrlResolver
    ) {
        List<ParticipantsPreviewItem> items = participationPage.getContent().stream()
                .map(p -> {
                    User user = p.getUser();
                    String profileImageUrl = imageUrlResolver.apply(user);
                    return ParticipantsPreviewItem.from(p, profileImageUrl);
                })
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