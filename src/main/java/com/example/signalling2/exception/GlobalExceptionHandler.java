package com.example.signalling2.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
//    @ExceptionHandler
//    public ResponseEntity<Object> handleApplicationErrorException(ApplicationErrorException exception) {
//        log.info(exception.getExceptionType().getMessage());
//        return new ResponseEntity<>(exception.getExceptionType(), exception.getExceptionType().getHttpStatus());
//    }

}
