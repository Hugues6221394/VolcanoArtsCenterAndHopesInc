package com.volcanoartscenter.platform.shared.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class BusinessRuleException extends PlatformException {
    public BusinessRuleException(String code, String message) {
        super(code, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public BusinessRuleException(String code, String message, Map<String, Object> details) {
        super(code, message, HttpStatus.UNPROCESSABLE_ENTITY, details);
    }
}
