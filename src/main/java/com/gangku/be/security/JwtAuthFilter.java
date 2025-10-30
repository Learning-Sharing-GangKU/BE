// src/main/java/com/gangku/BE/security/JwtAuthFilter.java
package com.gangku.be.security;

import com.gangku.be.domain.User;
import com.gangku.be.jwt.JwtTokenProvider;
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
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (
                path.startsWith("/api/v1/auth") ||
                        path.equals("/api/v1/users") ||
                        path.equals("/api/v1/signup") ||
                        path.equals("/api/v1/categories") && request.getMethod().equals("POST")
        )  {
            filterChain.doFilter(request, response); // ✅ 조기 리턴
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // ✅ 조기 리턴
            return;
        }

        String token = authHeader.substring(7);

        if (jwtTokenProvider.validateToken(token)) {
            try {
                String userIdStr = jwtTokenProvider.getUserIdFromToken(token);
                Long userId = Long.parseLong(userIdStr);
                User user = userRepository.findById(userId).orElse(null);

                if (user != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, null);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (NumberFormatException e) {
                System.err.println("토큰에서 사용자 ID 파싱 실패: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response); // ✅ 무조건 마지막 한 번만 실행
    }
}