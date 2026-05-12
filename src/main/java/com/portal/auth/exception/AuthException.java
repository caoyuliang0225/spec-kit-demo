package com.portal.auth.exception;

public class AuthException extends RuntimeException {
    private final String code;
    private final String field;

    public AuthException(String code, String message) {
        super(message);
        this.code = code;
        this.field = null;
    }

    public AuthException(String code, String message, String field) {
        super(message);
        this.code = code;
        this.field = field;
    }

    public String getCode() { return code; }
    public String getField() { return field; }
}
