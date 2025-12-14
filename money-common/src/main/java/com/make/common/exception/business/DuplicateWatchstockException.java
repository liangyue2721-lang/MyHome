package com.make.common.exception.business;

public class DuplicateWatchstockException extends RuntimeException {
    public DuplicateWatchstockException(String message) {
        super(message);
    }
}