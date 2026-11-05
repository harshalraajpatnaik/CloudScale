package com.cloudscale.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends CloudScaleException {
    public ConflictException(String code, String message) {
        super(code, message, HttpStatus.CONFLICT);
    }
}
