package com.example.signalling2.common.exception;

import com.example.signalling2.common.Constant;
import com.example.signalling2.common.exception.ServiceException;
import com.example.signalling2.common.exception.KurentoException;
import com.example.signalling2.common.exception.errcode.ErrorCode;
import com.example.signalling2.common.exception.errcode.ServiceErrorCode;
import com.example.signalling2.common.exception.errcode.KurentoErrCode;
import com.example.signalling2.controller.dto.Response.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.bcel.Const;
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
        ServiceErrorCode serviceErrorCode = e.getServiceErrorCode();
        log.error("[ERROR] " + serviceErrorCode.getMessage());
        return ResponseEntity.status(serviceErrorCode.getHttpStatus())
                .body(makeErrorResponse(serviceErrorCode));
    }

    /**
     * KurentoException을 핸들링하는 메서드
     */
    @ExceptionHandler(KurentoException.class)
    public ResponseEntity<Object> handleKurentoException(KurentoException e) {
        KurentoErrCode kurentoErrCode = e.getKurentoErrCode();
        log.error("[ERROR] " + kurentoErrCode.getMessage());
        return ResponseEntity.status(kurentoErrCode.getHttpStatus())
                .body(makeErrorResponse(kurentoErrCode));
    }

    /**
     * Client에 정형화된 에러 메세지를 송신한다.
     * @param errorCode
     * @return
     */
    private ErrorResponseDto makeErrorResponse(ErrorCode errorCode) {
        return ErrorResponseDto.builder()
                .name(errorCode.name())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }
}