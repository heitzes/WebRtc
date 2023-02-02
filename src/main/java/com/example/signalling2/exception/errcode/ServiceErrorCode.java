package com.example.signalling2.exception.errcode;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
@RequiredArgsConstructor
public enum ServiceErrorCode {
    NO_USER(HttpStatus.NOT_FOUND, "User not exists"),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
