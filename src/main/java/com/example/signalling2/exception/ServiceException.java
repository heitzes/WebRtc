package com.example.signalling2.exception;

import com.example.signalling2.exception.errcode.ServiceErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ServiceException extends RuntimeException {
    private final ServiceErrorCode serviceErrorCode;
}
