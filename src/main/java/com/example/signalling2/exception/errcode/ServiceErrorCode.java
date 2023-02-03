package com.example.signalling2.exception.errcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
@Getter
@RequiredArgsConstructor
public enum ServiceErrorCode implements ErrorCode {
    NO_USER(HttpStatus.NOT_FOUND, "User not exists."),
    NO_ROOM(HttpStatus.NOT_FOUND, "ROOM not exists."),
    ALREADY_IN(HttpStatus.BAD_REQUEST, "You already in streaming."),
    ALREADY_OUT(HttpStatus.BAD_REQUEST, "You already left streaming."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
