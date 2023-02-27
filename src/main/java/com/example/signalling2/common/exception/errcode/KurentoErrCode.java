package com.example.signalling2.common.exception.errcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum KurentoErrCode implements ErrorCode {
    KMS_NOT_CONNECTED(HttpStatus.INTERNAL_SERVER_ERROR, "KMS server is not available."),
    KMS_NO_PIPELINE(HttpStatus.INTERNAL_SERVER_ERROR, "KMS can't create new media pipeline."),
    KMS_NO_ENDPOINT(HttpStatus.INTERNAL_SERVER_ERROR, "KMS can't create new webRtcPipeline."),
    KMS_NO_CONNECT(HttpStatus.INTERNAL_SERVER_ERROR, "KMS can't connect to presenter endpoint.")
    ;
    private final HttpStatus httpStatus;
    private final String message;

    KurentoErrCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
