package com.gangku.be.dto.gathering.response;

import com.gangku.be.domain.Participation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

//모임 상세 조회시, 참여자 정렬을 위한 Dto
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantPreviewDto {
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String role;
    private LocalDateTime joinedAt;

    public static ParticipantPreviewDto from(Participation p) {
        return new ParticipantPreviewDto(
                p.getUser().getId(),
                p.getUser().getNickname(),
                p.getUser().getProfileImageUrl(),
                p.getRole().name().toLowerCase(),
                p.getJoinedAt()
        );
    }
}
