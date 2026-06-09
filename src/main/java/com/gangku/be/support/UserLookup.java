package com.gangku.be.support;

import com.gangku.be.domain.User;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.UserErrorCode;
import com.gangku.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserLookup {

    private final UserRepository userRepository;

    public User findById(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }
}
