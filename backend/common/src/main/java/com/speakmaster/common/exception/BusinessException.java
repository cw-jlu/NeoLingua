package com.speakmaster.common.exception;

import com.speakmaster.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 业务异常�?
 * 用于业务逻辑中抛出的异常
 * 
 * @author SpeakMaster
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误�?
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String msg;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.msg = errorCode.getMsg();
    }

    public BusinessException(Integer code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public BusinessException(String msg) {
        super(msg);
        this.code = 500;
        this.msg = msg;
    }
}
