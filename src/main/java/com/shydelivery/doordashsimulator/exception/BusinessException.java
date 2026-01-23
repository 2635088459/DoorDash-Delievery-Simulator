package com.shydelivery.doordashsimulator.exception;

/**
 * 业务逻辑异常
 * 当业务规则验证失败时抛出（如邮箱已存在、权限不足等）
 */
public class BusinessException extends RuntimeException {
    
    public BusinessException(String message) {
        super(message);
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
