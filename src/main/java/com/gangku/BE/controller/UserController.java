// src/main/java/com/gangku/BE/controller/UserController.java

package com.gangku.BE.controller;

import com.gangku.BE.domain.User;
import com.gangku.BE.dto.SignupRequestDto;
import com.gangku.BE.dto.SignupResponseDto;
import com.gangku.BE.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

// REST API 컨트롤러임을 나타냄 → 모든 메서드는 JSON으로 응답
@RestController
// 해당 클래스의 모든 요청 경로 앞에 붙는 prefix
@RequestMapping("/api/v1/users")
// final 필드를 자동 생성자 주입 + 필드 초기화
@RequiredArgsConstructor
public class UserController {

    // 서비스 레이어 의존성 주입
    private final UserService userService;

    // POST 요청으로 회원가입 처리
    @PostMapping
    public ResponseEntity<?> registerUser(
            // @RequestBody: 요청 본문에서 JSON → Java 객체로 매핑
            // @Validated: 유효성 검증 수행
            @RequestBody @Validated SignupRequestDto requestDto) {

        // 회원가입 처리 후 저장된 User 엔티티 반환
        User savedUser = userService.registerUser(requestDto);

        // 응답 헤더에 Location 지정 (RESTful API 스타일)
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/api/v1/users/" + savedUser.getUserId()));

        // 응답 바디에 일부 정보 포함 (SignupResponseDto로 분리 가능)
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .headers(headers)
                .body(SignupResponseDto.from(savedUser));
    }
    }
