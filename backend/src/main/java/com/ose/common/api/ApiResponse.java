package com.ose.common.api;

import java.time.OffsetDateTime;

public record ApiResponse<T>(int code, String message, T data, OffsetDateTime timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "ok", data, OffsetDateTime.now());
    }

    public static ApiResponse<Void> successMessage(String message) {
        return new ApiResponse<>(0, message, null, OffsetDateTime.now());
    }

    public static ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>(code, message, null, OffsetDateTime.now());
    }
}
