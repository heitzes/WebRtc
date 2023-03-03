package com.example.signalling2.controller.dto.Response;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@Getter
@Builder
@RequiredArgsConstructor
public class ErrorResponseDto {
    private final String name;
    private final int code;
    private final String message;
}