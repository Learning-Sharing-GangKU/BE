package com.gangku.be.dto.common;


import lombok.AllArgsConstructor;
import lombok.Getter;

//성공한 응답을 감싸는 generic wrapper

@Getter
@AllArgsConstructor
public class DataResponseDto<T> {
    private T data;
}