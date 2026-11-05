package com.cloudscale.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends CloudScaleException {
    public ResourceNotFoundException(String code, String message) {
        super(code, message, HttpStatus.NOT_FOUND);
    }
}
