package com.example.signalling2.common.exception.errcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
@Getter
@RequiredArgsConstructor
public enum ServiceErrorCode implements ErrorCode {
    NO_USER(HttpStatus.BAD_REQUEST, "User not exists."),
    NO_ROOM(HttpStatus.BAD_REQUEST, "ROOM not exists."),
    ALREADY_IN(HttpStatus.CONFLICT, "You already in streaming."),
    ALREADY_OUT(HttpStatus.CONFLICT, "You already left streaming."),
    NO_SESSION(HttpStatus.BAD_REQUEST, "WebSocket Session Id not exists.")
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
