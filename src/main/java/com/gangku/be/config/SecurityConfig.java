package com.gangku.be.config;

import com.gangku.be.exception.JwtAccessDeniedHandler;
import com.gangku.be.exception.JwtAuthenticationEntryPoint;
import com.gangku.be.util.jwt.JwtAuthFilter;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final Environment environment;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        boolean isLocal = Arrays.asList(environment.getActiveProfiles()).contains("local");

        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        ex ->
                                ex.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(
                        auth -> {
                            auth.requestMatchers("/error")
                                    .permitAll()
                                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                                    .permitAll();

                            if (isLocal) {
                                auth.requestMatchers(PathRequest.toH2Console()).permitAll();
                            }

                            auth
                                    // 인증 및 회원가입 관련
                                    .requestMatchers("/api/v1/auth/**")
                                    .permitAll()
                                    .requestMatchers(HttpMethod.POST, "/api/v1/users")
                                    .permitAll()

                                    // 이메일 성공 및 실패 정적 파일 서빙인데, 로컬에서 테스트 용으로 열어둠
                                    .requestMatchers("/email/verification/**")
                                    .permitAll()

                                    // 공개 데이터 - GET만 허용
                                    .requestMatchers(
                                            HttpMethod.GET,
                                            "/api/v1/categories",
                                            "/api/v1/home",
                                            "/api/v1/gatherings")
                                    .permitAll()

                                    // 이미지 업로드 (회원 가입 시 이용)
                                    .requestMatchers(
                                            HttpMethod.POST, "/api/v1/objects/presigned-url/**")
                                    .permitAll()

                                    // 이외에는 로그인 필요
                                    .anyRequest()
                                    .authenticated();
                        })
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(
                        h -> {
                            if (isLocal) {
                                h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin);
                            }
                        });

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
