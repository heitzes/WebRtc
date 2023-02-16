package com.example.signalling2.handler;

import com.example.signalling2.exception.ServiceException;
import com.example.signalling2.exception.KurentoException;
import com.example.signalling2.exception.errcode.ServiceErrorCode;
import com.example.signalling2.exception.errcode.KurentoErrCode;
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
        System.out.println(serviceErrorCode.getMessage());
        return ResponseEntity.status(serviceErrorCode.getHttpStatus())
                .body(serviceErrorCode.getMessage());
    }
    /** KurentoException을 핸들링하는 메서드 */
    @ExceptionHandler(KurentoException.class)
    public ResponseEntity<Object> handleKurentoException(KurentoException e) {
        System.out.println("kurento exception handled");
        KurentoErrCode kurentoErrCode = e.getKurentoErrCode();
        System.out.println(kurentoErrCode.getMessage());
        return ResponseEntity.status(kurentoErrCode.getHttpStatus())
                .body(kurentoErrCode.getMessage());
    }
}
