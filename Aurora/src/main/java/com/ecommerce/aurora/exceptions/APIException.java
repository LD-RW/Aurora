package com.ecommerce.aurora.exceptions;

public class APIException extends RuntimeException{
    public static final long serialVersionId = 1L;

    public APIException() {
    }

    public APIException(String message) {
        super(message);
    }
}
