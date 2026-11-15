package com.example.labmcp.dto;

public record ToolResponse<T>(boolean success, String code, String message, T data) {

    public static <T> ToolResponse<T> success(String message, T data) {
        return new ToolResponse<>(true, "OK", message, data);
    }

    public static <T> ToolResponse<T> failure(String code, String message) {
        return new ToolResponse<>(false, code, message, null);
    }
}
