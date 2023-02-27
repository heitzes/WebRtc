package com.example.signalling2.common.exception;

import com.example.signalling2.common.exception.errcode.KurentoErrCode;
import lombok.Getter;

@Getter
public class KurentoException extends RuntimeException {
    private KurentoErrCode kurentoErrCode;
    public KurentoException(KurentoErrCode kurentoErrCode) {
        super(kurentoErrCode.getMessage());
        this.kurentoErrCode = kurentoErrCode;
    }
}
