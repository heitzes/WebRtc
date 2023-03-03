package com.example.signalling2.common.exception.errcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
@Getter
public enum ServiceErrorCode implements ErrorCode {
    NO_USER(HttpStatus.BAD_REQUEST, 5101, "User not exists."),
    NO_ROOM(HttpStatus.BAD_REQUEST,  5102, "ROOM not exists."),
    ALREADY_IN(HttpStatus.CONFLICT, 5103, "You already in streaming."),
    ALREADY_OUT(HttpStatus.CONFLICT, 5104, "You already left streaming."),
    NO_SESSION(HttpStatus.BAD_REQUEST, 5105, "WebSocket Session Id not exists.")
    ;

    private final HttpStatus httpStatus;
    private final int code;
    private final String message;
    ServiceErrorCode(HttpStatus httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
