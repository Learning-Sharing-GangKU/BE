package com.gangku.be.dto.gathering;

import com.gangku.be.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class HostDto {
    private Long id;
    private String nickname;

    public static HostDto from(User user) {
        return new HostDto(user.getId(), user.getNickname());
    }
}