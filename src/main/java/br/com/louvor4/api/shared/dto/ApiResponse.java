package br.com.louvor4.api.shared.dto;

import java.time.Instant;

public class ApiResponse<T> {

    private Instant timestamp;
    private int status;
    private String title;
    private String message;
    private T data;

    public ApiResponse() {
        this.timestamp = Instant.now();
    }

    public static <T> ApiResponse<T> create() {
        return new ApiResponse<>();
    }

    public ApiResponse<T> withStatus(int status) {
        this.status = status;
        return this;
    }

    public ApiResponse<T> withTitle(String title) {
        this.title = title;
        return this;
    }

    public ApiResponse<T> withMessage(String message) {
        this.message = message;
        return this;
    }

    public ApiResponse<T> withData(T data) {
        this.data = data;
        return this;
    }

    // Getters
    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }


    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
