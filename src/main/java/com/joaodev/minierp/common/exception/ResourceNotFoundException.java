package com.joaodev.minierp.common.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(UUID id) {
        super("Customer not found with id " + id);
    }
}
