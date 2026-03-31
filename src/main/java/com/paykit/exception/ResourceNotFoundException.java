package com.paykit.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super(String.format("%s not found: %s", resource, identifier));
    }
}
