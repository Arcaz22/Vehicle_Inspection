package com.vehicle.common;

import jakarta.ws.rs.core.Response;

import java.util.List;

public class ApiException extends RuntimeException {
    private final Response.Status status;
    private final List<ApiError> errors;

    public ApiException(Response.Status status, String message, List<ApiError> errors) {
        super(message);
        this.status = status;
        this.errors = errors;
    }

    public Response.Status status() {
        return status;
    }

    public List<ApiError> errors() {
        return errors;
    }

    public static ApiException notFound(String message) {
        return new ApiException(Response.Status.NOT_FOUND, message, List.of());
    }

    public static ApiException validation(List<ApiError> errors) {
        return new ApiException(Response.Status.BAD_REQUEST, "Validation failed", errors);
    }

    public static ApiException conflict(String message) {
        return new ApiException(Response.Status.CONFLICT, message, List.of());
    }
}
