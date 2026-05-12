package com.portal.auth.dto.response;

import java.time.Instant;

public class ErrorResponse {
    private String code;
    private String message;
    private String field;
    private Instant timestamp;

    public ErrorResponse() {
        this.timestamp = Instant.now();
    }

    public ErrorResponse(String code, String message) {
        this();
        this.code = code;
        this.message = message;
    }

    public ErrorResponse(String code, String message, String field) {
        this(code, message);
        this.field = field;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public Instant getTimestamp() { return timestamp; }
}
