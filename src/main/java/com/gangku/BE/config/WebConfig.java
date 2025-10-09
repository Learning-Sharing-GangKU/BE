package com.gangku.BE.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // 모든 경로에 대해
                        .allowedOrigins("http://localhost:3000") // 프론트 서버 주소
                        .allowedMethods("*") // 모든 HTTP 메서드 허용 (POST, GET 등)
                        .allowedHeaders("*") // 모든 헤더 허용
                        .allowCredentials(true); // 쿠키, 인증정보 포함 허용
            }
        };
    }
}