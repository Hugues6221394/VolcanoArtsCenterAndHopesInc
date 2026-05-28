package com.volcanoartscenter.platform.shared.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends PlatformException {
    public NotFoundException(String resource, Object id) {
        super("RESOURCE_NOT_FOUND", resource + " not found: " + id, HttpStatus.NOT_FOUND);
    }

    public NotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
