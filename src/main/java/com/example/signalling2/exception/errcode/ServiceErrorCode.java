package com.example.signalling2.exception.errcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
@Getter
@RequiredArgsConstructor
public enum ServiceErrorCode implements ErrorCode {
    NO_USER(HttpStatus.NO_CONTENT, "User not exists."),
    NO_ROOM(HttpStatus.NO_CONTENT, "ROOM not exists."),
    ALREADY_IN(HttpStatus.CONFLICT, "You already in streaming."),
    ALREADY_OUT(HttpStatus.NO_CONTENT, "You already left streaming."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
