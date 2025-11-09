package edu.xtu.bbs.common.exception;

import edu.xtu.bbs.common.response.ResponseCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Base business exception class
 * All business-related exceptions should inherit from this class
 */

@Getter
@ToString
public class BusinessException extends RuntimeException {

    private final ResponseCode responseCode;
    private final String customMessage;

    public BusinessException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.responseCode = responseCode;
        this.customMessage = null;
    }

    public BusinessException(ResponseCode responseCode, String customMessage) {
        super(customMessage);
        this.responseCode = responseCode;
        this.customMessage = customMessage;
    }

    public BusinessException(ResponseCode responseCode, Throwable cause) {
        super(responseCode.getMessage(), cause);
        this.responseCode = responseCode;
        this.customMessage = null;
    }

    public BusinessException(ResponseCode responseCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.responseCode = responseCode;
        this.customMessage = customMessage;
    }

    /**
     * Get the final message to display
     * Returns custom message if available, otherwise returns default message from response code
     */
    public String getFinalMessage() {
        return customMessage != null ? customMessage : responseCode.getMessage();
    }

}