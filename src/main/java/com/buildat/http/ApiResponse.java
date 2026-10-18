package com.buildat.http;

public record ApiResponse<T>(String requestId, T data) { }
