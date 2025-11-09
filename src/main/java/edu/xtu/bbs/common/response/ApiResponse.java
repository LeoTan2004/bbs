package edu.xtu.bbs.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.xtu.bbs.common.trace.TraceIdUtil;
import lombok.Data;

/**
 * Unified API Response Format
 *
 * @param <T> data type
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    // Getters and Setters
    /**
     * Response code
     */
    private Integer code;

    /**
     * Response message
     */
    private String message;

    /**
     * Response data
     */
    private T data;

    /**
     * Timestamp
     */
    private Long timestamp;

    /**
     * Trace ID for request tracking
     */
    private String traceId;

    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
        this.traceId = TraceIdUtil.getTraceId(); // Automatically get current thread's TraceId
    }

    public ApiResponse(Integer code, String message) {
        this();
        this.code = code;
        this.message = message;
    }

    public ApiResponse(Integer code, String message, T data) {
        this(code, message);
        this.data = data;
    }

    // Success responses
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMessage());
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMessage(), data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ResponseCode.SUCCESS.getCode(), message, data);
    }

    // Error responses
    public static <T> ApiResponse<T> error(ResponseCode responseCode) {
        return new ApiResponse<>(responseCode.getCode(), responseCode.getMessage());
    }

    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message);
    }

    public static <T> ApiResponse<T> error(ResponseCode responseCode, String customMessage) {
        return new ApiResponse<>(responseCode.getCode(), customMessage);
    }

}