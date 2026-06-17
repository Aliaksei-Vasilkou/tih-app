package com.tih.app.util;

public class ErrorCode {

    public static final String DUPLICATE_RESOURCE = "DUPLICATE_RESOURCE";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String UNKNOWN_LANGUAGE = "UNKNOWN_LANGUAGE";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    private ErrorCode() {
        throw new IllegalStateException("Utility class can't be instantiated");
    }
}
