package com.gangku.be.config;

import com.gangku.be.jwt.JwtTokenProvider;
import com.gangku.be.repository.UserRepository;
import com.gangku.be.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@RequiredArgsConstructor
@Configuration
public class SecurityConfig {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.addFilterBefore(jwtAuthFilter(), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        http
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathRequest.toH2Console()).permitAll() // H2 콘솔은 인증 없이 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Preflight 요청 허용
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll() //회원가입 허용
                        .requestMatchers(HttpMethod.POST, "/api/v1/categories").permitAll() //카테고리 생성 허용
                        .requestMatchers("/api/v1/auth/**").permitAll() //로그인, 토큰 재발급, 로그아웃 등 인증 API 허용
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll() //카테고리 조회 인증없이 허용
                        .requestMatchers(HttpMethod.POST, "/api/v1/objects/presigned-url/**").permitAll()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(Customizer.withDefaults()).disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 비밀번호 암호화용 Encoder 등록
    }


    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(jwtTokenProvider, userRepository); // JwtAuthFilter 객체 등록
    }
}