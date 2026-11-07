package com.vehicle.common;

public record ApiResponse<T>(int statusCode, String message, T data, PageMeta meta) {
    public static <T> ApiResponse<T> of(int statusCode, String message, T data) {
        return new ApiResponse<>(statusCode, message, data, null);
    }

    public static <T> ApiResponse<T> page(int statusCode, String message, T data, PageMeta meta) {
        return new ApiResponse<>(statusCode, message, data, meta);
    }

    public record PageMeta(int page, int limit, long total) {}
}
