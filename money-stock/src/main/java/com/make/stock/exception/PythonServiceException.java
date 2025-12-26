package com.make.stock.exception;


public class PythonServiceException extends RuntimeException {
    private final int statusCode;
    private final String body;

    public PythonServiceException(int statusCode, String body) {
        super("Python service error: " + statusCode);
        this.statusCode = statusCode;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }
}
