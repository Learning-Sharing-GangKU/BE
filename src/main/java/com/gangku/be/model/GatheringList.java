package com.gangku.be.model;

import com.gangku.be.domain.Gathering;
import java.util.List;
import org.springframework.data.domain.Page;

public record GatheringList(
        List<GatheringListItem> data,
        PageMeta meta
) {
    public static GatheringList from(
            Page<Gathering> gatheringPage,
            int pageNumber,
            int size,
            String sortedBy
    ) {
        List<GatheringListItem> items = gatheringPage.getContent().stream()
                .map(GatheringListItem::from)
                .toList();

        PageMeta meta = PageMeta.from(
                gatheringPage,
                pageNumber,
                size,
                sortedBy
        );

        return new GatheringList(items, meta);
    }
}
