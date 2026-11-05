package com.cloudscale.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        ResponseMeta meta,
        PaginationMeta pagination
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, ResponseMeta.now(), null);
    }

    public static <T> ApiResponse<T> ok(T data, PaginationMeta pagination) {
        return new ApiResponse<>(true, data, null, ResponseMeta.now(), pagination);
    }

    public static <T> ApiResponse<T> error(String code, String message, Object details) {
        return new ApiResponse<>(false, null, new ApiError(code, message, details), ResponseMeta.now(), null);
    }

    public record ApiError(String code, String message, Object details) {}

    public record ResponseMeta(Instant timestamp, String requestId, String version) {
        public static ResponseMeta now() {
            return new ResponseMeta(Instant.now(), UUID.randomUUID().toString(), "v1");
        }
    }

    public record PaginationMeta(int page, int size, long totalElements, int totalPages, boolean isFirst, boolean isLast) {}
}
