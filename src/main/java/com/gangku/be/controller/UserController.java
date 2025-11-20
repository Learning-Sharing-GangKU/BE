package com.gangku.be.controller;

import com.gangku.be.domain.User;
import com.gangku.be.dto.user.SignUpRequestDto;
import com.gangku.be.dto.user.SignUpResponseDto;
import com.gangku.be.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<SignUpResponseDto> registerUser(@RequestBody @Valid SignUpRequestDto signUpRequestDto) {

        // 1) 회원가입 처리 후 저장된 유저 반환
        User newUser = userService.registerUser(signUpRequestDto);

        // 2) Location 헤더 설정을 위한 정보 저장
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newUser.getId())
                .toUri();

        return ResponseEntity.created(location).body(SignUpResponseDto.from(newUser));
    }
}
