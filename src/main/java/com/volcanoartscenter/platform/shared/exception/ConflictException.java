package com.volcanoartscenter.platform.shared.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ConflictException extends PlatformException {
    public ConflictException(String code, String message) {
        super(code, message, HttpStatus.CONFLICT);
    }

    public ConflictException(String code, String message, Map<String, Object> details) {
        super(code, message, HttpStatus.CONFLICT, details);
    }
}
