package com.example.signalling2.common.exception.errcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum KurentoErrCode implements ErrorCode {
    KMS_NO_PIPELINE(HttpStatus.INTERNAL_SERVER_ERROR,  5001, "KMS can't create new media pipeline."),
    KMS_NO_ENDPOINT(HttpStatus.INTERNAL_SERVER_ERROR, 5002, "KMS can't create new webRtcEndpoint."),
    KMS_RELEASE_ENDPOINT(HttpStatus.INTERNAL_SERVER_ERROR, 5003, "KMS can't release webRtcEndpoint."),
    KMS_RELEASE_PIPELINE(HttpStatus.INTERNAL_SERVER_ERROR, 5004,  "KMS can't release media pipeline."),
    KMS_RESTORE_ENDPOINT(HttpStatus.INTERNAL_SERVER_ERROR, 5005, "KMS can't restore webRtcEndpoint."),
    KMS_RESTORE_PIPELINE(HttpStatus.INTERNAL_SERVER_ERROR, 5006,"KMS can't restore media pipeline."),
    KMS_NO_CONNECT(HttpStatus.INTERNAL_SERVER_ERROR, 5007, "KMS can't connect to presenter endpoint.")
    ;
    private final HttpStatus httpStatus;
    private final int code;
    private final String message;

    KurentoErrCode(HttpStatus httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
