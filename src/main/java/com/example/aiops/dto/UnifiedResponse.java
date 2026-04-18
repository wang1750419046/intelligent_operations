package com.example.aiops.dto;

public class UnifiedResponse<T> {

    private final int code;
    private final String message;
    private final T data;
    private final String traceId;

    private UnifiedResponse(int code, String message, T data, String traceId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = traceId;
    }

    public static <T> UnifiedResponse<T> success(T data, String traceId) {
        return new UnifiedResponse<>(0, "success", data, traceId);
    }

    public static <T> UnifiedResponse<T> fail(int code, String message, String traceId) {
        return new UnifiedResponse<>(code, message, null, traceId);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getTraceId() {
        return traceId;
    }
}
