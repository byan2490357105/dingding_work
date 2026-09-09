package com.example.backend.exception;

import lombok.Getter;

/**
 * 业务异常，由全局异常处理器统一转为 Result 返回。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
