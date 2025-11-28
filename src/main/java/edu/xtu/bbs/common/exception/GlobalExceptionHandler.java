package edu.xtu.bbs.common.exception;

import edu.xtu.bbs.common.response.ApiResponse;
import edu.xtu.bbs.common.response.ResponseCode;
import edu.xtu.bbs.user.exception.*;
import edu.xtu.bbs.verification.VerificationExpiredException;
import edu.xtu.bbs.verification.VerificationRequestNotFoundException;
import edu.xtu.bbs.verification.VerificationScopeIncorrectException;
import edu.xtu.bbs.verification.VerificationTooFrequentException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Global Exception Handler
 * Handles all exceptions in the application and returns standardized response
 * format
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle business exceptions
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.toString());
        ApiResponse<Object> response = ApiResponse.error(e.getResponseCode(), e.getFinalMessage());
        return ResponseEntity.ok(response);
    }

    /**
     * Handle user not found exceptions
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserNotFoundException(UserNotFoundException e) {
        log.warn("User not found exception: {}", e.toString());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.USER_NOT_FOUND);
        return ResponseEntity.ok(response);
    }

    /**
     * Handle username not found exceptions
     */
    @ExceptionHandler(EmailNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmailNotFoundException(EmailNotFoundException e) {
        log.warn("Email not found exception: {}", e.toString());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.USER_NOT_FOUND);
        return ResponseEntity.ok(response);
    }

    /**
     * Handle username already occupied exceptions
     */
    @ExceptionHandler(UsernameOccupiedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUsernameOccupiedException(UsernameOccupiedException e) {
        log.warn("Username already occupied exception: {}", e.toString());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.USER_ALREADY_EXISTS);
        return ResponseEntity.ok(response);
    }

    /**
     * Handle username already exists exceptions
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmailAlreadyExistsException(EmailAlreadyExistsException e) {
        log.warn("Email already exists exception: {}", e.toString());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.USER_ALREADY_EXISTS);
        return ResponseEntity.ok(response);
    }

    /**
     * Handle invalid WeChat code exceptions
     */
    @ExceptionHandler(InvalidWeChatCodeException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidWechatCodeException(InvalidWeChatCodeException e) {
        log.warn("Invalid WeChat code exception: {}", e.toString());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.WECHAT_CODE_INVALID, "Invalid WeChat code");
        return ResponseEntity.ok(response);
    }

    /**
     * Handle invalid username exceptions
     */
    @ExceptionHandler(InvalidUsernameException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidUsernameException(InvalidUsernameException e) {
        log.warn("Invalid username exception: {}", e.toString());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.PARAM_INVALID, "Invalid username format");
        return ResponseEntity.ok(response);
    }

    /**
     * Handle WeChat OpenID already exists exceptions
     */
    @ExceptionHandler(WeChatOpenIdAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleWeChatOpenIdAlreadyExistsException(
            WeChatOpenIdAlreadyExistsException e) {
        log.warn("WeChat OpenID already exists exception: {}", e.toString());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.WECHAT_OPENID_ALREADY_EXISTS);
        return ResponseEntity.ok(response);
    }

    /**
     * Handle WeChat app user not found exceptions
     */
    @ExceptionHandler(WeChatAppUserNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleWeChatAppUserNotFoundException(WeChatAppUserNotFoundException e) {
        log.warn("WeChat app user not found exception: {}", e.toString());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.USER_NOT_FOUND, "WeChat user not found");
        return ResponseEntity.ok(response);
    }

    /**
     * Handle verification code expired exceptions
     */
    @ExceptionHandler(VerificationExpiredException.class)
    public ResponseEntity<ApiResponse<Object>> handleVerificationExpiredException(VerificationExpiredException e) {
        log.warn("Verification code expired exception: {}", e.toString());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.PARAM_INVALID, "Verification code has expired");
        return ResponseEntity.ok(response);
    }

    /**
     * Handle verification request too frequent exceptions
     */
    @ExceptionHandler(VerificationTooFrequentException.class)
    public ResponseEntity<ApiResponse<Object>> handleVerificationTooFrequentException(
            VerificationTooFrequentException e) {
        log.warn("Verification request too frequent exception: {}", e.toString());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.TOO_MANY_REQUESTS);
        return ResponseEntity.ok(response);
    }

    /**
     * Handle verification scope incorrect exceptions
     */
    @ExceptionHandler(VerificationScopeIncorrectException.class)
    public ResponseEntity<ApiResponse<Object>> handleVerificationScopeIncorrectException(
            VerificationScopeIncorrectException e) {
        log.warn("Verification scope incorrect exception: {}", e.toString());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.PARAM_INVALID, "Incorrect verification scope");
        return ResponseEntity.ok(response);
    }

    /**
     * Handle verification request not found exceptions
     */
    @ExceptionHandler(VerificationRequestNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleVerificationRequestNotFoundException(
            VerificationRequestNotFoundException e) {
        log.warn("Verification request not found exception: {}", e.toString());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.PARAM_INVALID, "Verification request not found");
        return ResponseEntity.ok(response);
    }

    /**
     * Handle invalid verification exceptions
     */
    @ExceptionHandler(InvalidVerificationException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidVerificationException(InvalidVerificationException e) {
        log.warn("Invalid verification exception: {}", e.toString());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.PARAM_INVALID, "Invalid verification code");
        return ResponseEntity.ok(response);
    }

    /**
     * Handle file size limit exceeded exceptions
     */
    @ExceptionHandler(FileSizeLimitExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleFileSizeLimitExceededException(FileSizeLimitExceededException e) {
        log.warn("File size limit exceeded exception: {}", e.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.FILE_UPLOAD_ERROR, "File size exceeds limit");
        return ResponseEntity.ok(response);
    }

    /**
     * Handle illegal content type exceptions
     */
    @ExceptionHandler(IllegalContentTypeException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalContentTypeException(IllegalContentTypeException e) {
        log.warn("Illegal content type exception: {}", e.toString());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.FILE_UPLOAD_ERROR, "Unsupported file type");
        return ResponseEntity.ok(response);
    }

    /**
     * Handle JWT token invalidation exceptions
     */
    @ExceptionHandler(JwtTokenInvalidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleJwtTokenInvalidationException(JwtTokenInvalidationException e) {
        log.warn("JWT token invalidation exception: {}", e.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.UNAUTHORIZED, "Invalid token");
        return ResponseEntity.ok(response);
    }

    /**
     * Handle Spring Security authentication exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(AuthenticationException e) {
        log.warn("Authentication exception: {}", e.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.UNAUTHORIZED);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle Spring Security access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied exception: {}", e.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.FORBIDDEN);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle Spring Security bad credentials exceptions
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("Bad credentials exception: {}", e.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.PASSWORD_INCORRECT);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle generic security exceptions
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Object>> handleSecurityException(SecurityException e) {
        log.warn("Security exception: {}", e.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.FORBIDDEN, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle parameter validation exceptions (triggered by @Valid annotation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        log.warn("Parameter validation exception: {}", e.getMessage());

        return getApiResponseResponseEntity(e.getBindingResult());
    }

    private ResponseEntity<ApiResponse<Object>> getApiResponseResponseEntity(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : bindingResult.getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ApiResponse<Object> response = ApiResponse.error(ResponseCode.PARAM_INVALID);
        response.setData(errors);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle Bean binding exceptions
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Object>> handleBindException(BindException e) {
        log.warn("Parameter binding exception: {}", e.getMessage());

        return getApiResponseResponseEntity(e.getBindingResult());
    }

    /**
     * Handle constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("Constraint violation exception: {}", e.getMessage());

        Map<String, String> errors = new HashMap<>();
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        for (ConstraintViolation<?> violation : violations) {
            String fieldName = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(fieldName, message);
        }

        ApiResponse<Object> response = ApiResponse.error(ResponseCode.PARAM_INVALID);
        response.setData(errors);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle request method not supported exceptions
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        log.warn("Request method not supported exception: {}", e.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.PARAM_INVALID, "Request method not allowed");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * Handle missing request parameter exceptions
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e) {
        log.warn("Missing request parameter exception: {}", e.getMessage());
        String message = String.format("Missing required request parameter: %s", e.getParameterName());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.PARAM_INVALID, message);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle method argument type mismatch exceptions
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        log.warn("Method argument type mismatch exception: {}", e.getMessage());
        String message = String.format("Parameter %s with value %s has incorrect type", e.getName(), e.getValue());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.PARAM_INVALID, message);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle HTTP message not readable exceptions
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e) {
        log.warn("HTTP message not readable exception: {}", e.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.PARAM_INVALID, "Request body format error");
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle NoResourceFoundException (Spring 6.x)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("No resource found exception: {}", e.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.PARAM_INVALID, "Resource not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle 404 exceptions
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("404 exception: {}", e.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.PARAM_INVALID, "Resource not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle IllegalStateException
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalStateException(IllegalStateException e) {
        log.warn("Illegal state exception: {}", e.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR, e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle all uncaught exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(Exception e) {
        log.error("Uncaught exception: ", e);
        ApiResponse<Object> response = ApiResponse.error(ResponseCode.SYSTEM_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}