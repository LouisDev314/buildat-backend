package com.buildat.http;

// Safe public information only; never store upstream error bodies or exception messages here.
public final class ApiFailure extends RuntimeException {
    private final int status;
    private final String code;
    public ApiFailure(int status, String code) {
        super(code);
        this.status = status;
        this.code = code;
    }
    public int status() { return status; }
    public String code() { return code; }
}
