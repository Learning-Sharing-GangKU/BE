package com.gangku.be.support;

import com.gangku.be.domain.Gathering;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.GatheringErrorCode;
import com.gangku.be.repository.GatheringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GatheringLookup {

    private final GatheringRepository gatheringRepository;

    public Gathering findById(Long gatheringId) {
        return gatheringRepository
                .findById(gatheringId)
                .orElseThrow(() -> new CustomException(GatheringErrorCode.GATHERING_NOT_FOUND));
    }
}
