package com.paykit.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String idempotencyKey) {
        super(String.format("Duplicate request with idempotency key: %s", idempotencyKey));
    }
}
