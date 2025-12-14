package com.make.common.exception.business;

/**
 * 自定义业务异常类，用于处理业务逻辑相关的异常。
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
