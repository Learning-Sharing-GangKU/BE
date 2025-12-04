package com.gangku.be.model.gathering;

import com.gangku.be.domain.User;

public record HostSummary(
        Long id,
        String nickname
) {
    public static HostSummary from(User user) {
        return new HostSummary(
                user.getId(),
                user.getNickname()
        );
    }
}
