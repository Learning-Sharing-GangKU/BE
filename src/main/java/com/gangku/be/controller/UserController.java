// src/main/java/com/gangku/BE/controller/UserController.java

package com.gangku.be.controller;

import com.gangku.be.domain.User;
import com.gangku.be.dto.user.SignupRequestDto;
import com.gangku.be.dto.user.SignupResponseDto;
import com.gangku.be.service.UserService;
import com.gangku.be.service.PreferredCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    // 서비스 레이어 의존성 주입
    private final UserService userService;
    private final PreferredCategoryService preferredCategoryService;

    // POST 요청으로 회원가입 처리
    @PostMapping
    public ResponseEntity<?> registerUser(
            // @RequestBody: 요청 본문에서 JSON → Java 객체로 매핑
            // @Validated: 유효성 검증 수행
            @RequestBody @Validated SignupRequestDto requestDto) {

        //  회원가입 처리 후 저장된 유저 반환
        User savedUser = userService.registerUser(requestDto);

        //  방금 저장된 유저의 선호 카테고리 목록 조회
        List<String> preferredNames = preferredCategoryService.getPreferredCategoryNames(savedUser.getId());

        //  응답 DTO 변환 (usr_123 + 카테고리 목록 포함)
        SignupResponseDto response = SignupResponseDto.from(savedUser, preferredNames);

        //  Location 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/api/v1/users/" + savedUser.getId()));



        //  응답 반환
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .headers(headers)
                .body(response);


    }
    }
