package com.guide.common.exception;

import com.guide.common.api.ErrorCode;
import lombok.Getter;

/**
 * 业务异常：携带错误码，由全局异常处理器统一转 Result。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.code = errorCode.getCode();
    }
}
