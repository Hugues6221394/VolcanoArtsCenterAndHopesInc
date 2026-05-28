package com.volcanoartscenter.platform.shared.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class PlatformException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final Map<String, Object> details;

    public PlatformException(String code, String message, HttpStatus status) {
        this(code, message, status, null);
    }

    public PlatformException(String code, String message, HttpStatus status, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
