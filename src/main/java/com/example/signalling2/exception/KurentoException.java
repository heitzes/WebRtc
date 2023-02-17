package com.example.signalling2.exception;

import com.example.signalling2.exception.errcode.KurentoErrCode;
import lombok.Getter;

@Getter
public class KurentoException extends RuntimeException {
    private KurentoErrCode kurentoErrCode;
    public KurentoException(KurentoErrCode kurentoErrCode) {
        super(kurentoErrCode.getMessage());
        this.kurentoErrCode = kurentoErrCode;
    }
}
