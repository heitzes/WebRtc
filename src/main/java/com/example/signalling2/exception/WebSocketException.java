package com.example.signalling2.exception;

import com.example.signalling2.exception.errcode.WebSocketErrCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class WebSocketException extends Exception {
    private WebSocketErrCode webSocketErrCode;
    public WebSocketException(WebSocketErrCode webSocketErrCode) {
        super(webSocketErrCode.getMessage());
        this.webSocketErrCode = webSocketErrCode;
    }
}
