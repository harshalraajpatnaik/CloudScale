package com.cloudscale.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends CloudScaleException {
    public UnauthorizedException(String code, String message) {
        super(code, message, HttpStatus.UNAUTHORIZED);
    }
}
