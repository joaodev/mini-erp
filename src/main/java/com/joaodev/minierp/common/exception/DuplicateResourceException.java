package com.joaodev.minierp.common.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String document) {
        super("Customer already exists with document " + document);
    }
}
