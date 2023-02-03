package com.example.signalling2.handler;

import com.example.signalling2.exception.ServiceException;
import com.example.signalling2.exception.WebSocketException;
import com.example.signalling2.exception.errcode.ServiceErrorCode;
import com.example.signalling2.exception.errcode.WebSocketErrCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.socket.WebSocketSession;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    /** ServiceException을 핸들링하는 메서드 */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Object> handleServiceException(ServiceException e) {
        System.out.println("service exception handled");
        ServiceErrorCode serviceErrorCode = e.getServiceErrorCode();
        return ResponseEntity.status(serviceErrorCode.getHttpStatus())
                .body(serviceErrorCode.getMessage());
    }
    /** WebSocketException을 핸들링하는 메서드 */
    @ExceptionHandler(WebSocketException.class)
    public ResponseEntity<Object> handleWebSocketException(WebSocketException e, WebSocketSession session) {
        System.out.println("websocket exception handled");
        WebSocketErrCode webSocketErrorCode = e.getWebSocketErrCode();
        return ResponseEntity.status(webSocketErrorCode.getHttpStatus())
                .body(webSocketErrorCode.getMessage());
    }
}
