package com.gangku.be.model.gathering;

import com.gangku.be.domain.Gathering;
import com.gangku.be.model.common.PageMeta;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record GatheringList(
        List<GatheringListItem> data,
        PageMeta meta
) {
    public static GatheringList from(
            Page<Gathering> gatheringPage,
            int pageNumber,
            int size,
            String sortedBy,
            Function<Gathering, String> imageUrlResolver
    ) {
        List<GatheringListItem> items = gatheringPage.getContent().stream()
                .map(g -> GatheringListItem.from(g, imageUrlResolver.apply(g)))
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
