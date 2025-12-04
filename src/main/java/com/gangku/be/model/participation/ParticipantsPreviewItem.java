package com.gangku.be.model.participation;

import com.gangku.be.domain.Participation;
import com.gangku.be.domain.User;
import java.time.LocalDateTime;

public record ParticipantsPreviewItem(
        Long userId,
        String nickname,
        String profileImageUrl,
        String role,
        LocalDateTime joinedAt
) {
    public static ParticipantsPreviewItem from(Participation participation, String profileImageUrl) {
        User user = participation.getUser();

        return new ParticipantsPreviewItem(
                user.getId(),
                user.getNickname(),
                profileImageUrl,
                participation.getRole().name(),
                participation.getJoinedAt()
        );
    }
}
