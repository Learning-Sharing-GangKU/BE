package com.gangku.be.service;

import com.gangku.be.domain.User;
import com.gangku.be.dto.user.LoginRequestDto;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.security.jwt.JwtTokenProvider;
import com.gangku.be.security.jwt.TokenPair;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    // 로그인 → JWT 토큰 생성
    public TokenPair login(LoginRequestDto loginRequestDto) {

        // 1) 이메일 / 비밀번호 검증
        User user = authenticate(loginRequestDto.getEmail(), loginRequestDto.getPassword());

        // 2) Token 생성
        String accessToken = jwtTokenProvider.generateAccessToken(String.valueOf(user.getId()));
        String refreshToken = jwtTokenProvider.generateRefreshToken(String.valueOf(user.getId()));

        // 3) Refresh Token DB에 갱신
        user.updateRefreshToken(refreshToken, LocalDateTime.now().plusDays(14));
        userRepository.save(user);

        // 4) TokenPair DTO 반환
        return new TokenPair(accessToken, refreshToken);
    }

    // 로그인 (비밀번호 체크)
    private User authenticate(String email, String rawPassword) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return user;
    }
}
