package com.shydelivery.doordashsimulator.exception;

/**
 * 资源未找到异常
 * 当查询的资源（用户、订单等）不存在时抛出
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
    }
}
