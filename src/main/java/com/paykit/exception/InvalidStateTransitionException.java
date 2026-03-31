package com.paykit.exception;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String from, String to) {
        super(String.format("Invalid state transition: %s -> %s", from, to));
    }
}
