// src/main/java/com/gangku/BE/security/JwtAuthFilter.java
package com.gangku.be.security.jwt;

import com.gangku.be.domain.User;
import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.ErrorCode;
import com.gangku.be.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        System.out.println("[JwtAuthFilter] 요청 경로: " + method + " " + path);

        // 1. OPTIONS 요청은 허용
        if ("OPTIONS".equalsIgnoreCase(method)) {
            System.out.println("[JwtAuthFilter] OPTIONS 요청 허용");
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 인증 제외 경로
        if (
                path.startsWith("/api/v1/auth") ||
                        path.equals("/api/v1/users") ||
                        path.equals("/api/v1/signup") ||
                        (path.equals("/api/v1/categories") && method.equals("POST"))
        ) {
            System.out.println("[JwtAuthFilter] 인증 제외 경로 허용: " + path);
            filterChain.doFilter(request, response);
            return;
        }


        String authHeader = request.getHeader("Authorization");
        System.out.println("[JwtAuthFilter] Authorization 헤더: " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("[JwtAuthFilter] 유효하지 않은 Authorization 헤더. 인증 건너뜀");
            filterChain.doFilter(request, response); // ✅ 조기 리턴
            return;
        }

        String token = authHeader.substring(7);
        if (jwtTokenProvider.validateToken(token)) {
            try {
                String userIdStr = jwtTokenProvider.getUserIdFromToken(token);
                System.out.println("[JwtAuthFilter] 토큰에서 추출한 userId: " + userIdStr);

                Long userId = Long.parseLong(userIdStr);
                User user = userRepository.findById(userId).orElse(null);

                if (user != null) {
                    System.out.println("[JwtAuthFilter] 유저 인증 성공: " + user.getEmail());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, null);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }else {
                    System.out.println("[JwtAuthFilter] 유저 정보 없음");
                }
            } catch (NumberFormatException e) {
                System.err.println("토큰에서 사용자 ID 파싱 실패: " + e.getMessage());
            }
        }else {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        filterChain.doFilter(request, response); // ✅ 무조건 마지막 한 번만 실행
    }
}