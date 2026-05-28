package com.volcanoartscenter.platform.shared.web.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        ApiError error,
        String timestamp,
        String requestId
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, Instant.now().toString(), MDC.get("requestId"));
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null, Instant.now().toString(), MDC.get("requestId"));
    }

    public static ApiResponse<Void> failure(ApiError error) {
        return new ApiResponse<>(false, null, null, error, Instant.now().toString(), MDC.get("requestId"));
    }
}
