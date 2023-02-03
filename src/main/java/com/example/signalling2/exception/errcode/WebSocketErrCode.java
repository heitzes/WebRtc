package com.example.signalling2.exception.errcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
public enum WebSocketErrCode implements ErrorCode {
    KMS_NOT_CONNECTED(HttpStatus.INTERNAL_SERVER_ERROR, "KMS server is not available."),
    SESSION_CLOSED(HttpStatus.INTERNAL_SERVER_ERROR,"WebSocket Session disconnected"),
    ;
    private final HttpStatus httpStatus;
    private final String message;

    WebSocketErrCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
