package com.example.signalling2.common.exception;

import com.example.signalling2.common.exception.ServiceException;
import com.example.signalling2.common.exception.KurentoException;
import com.example.signalling2.common.exception.errcode.ServiceErrorCode;
import com.example.signalling2.common.exception.errcode.KurentoErrCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    /**
     * ServiceException을 핸들링하는 메서드
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Object> handleServiceException(ServiceException e) {
        log.error("service exception handled");
        ServiceErrorCode serviceErrorCode = e.getServiceErrorCode();
        log.error(serviceErrorCode.getMessage());
        return ResponseEntity.status(serviceErrorCode.getHttpStatus())
                .body(serviceErrorCode.getMessage());
    }

    /**
     * KurentoException을 핸들링하는 메서드
     */
    @ExceptionHandler(KurentoException.class)
    public ResponseEntity<Object> handleKurentoException(KurentoException e) {
        log.error("kurento exception handled");
        KurentoErrCode kurentoErrCode = e.getKurentoErrCode();
        log.error(kurentoErrCode.getMessage());
        return ResponseEntity.status(kurentoErrCode.getHttpStatus())
                .body(kurentoErrCode.getMessage());
    }
}
