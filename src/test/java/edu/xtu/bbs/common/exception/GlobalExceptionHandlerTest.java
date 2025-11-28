package edu.xtu.bbs.common.exception;

import edu.xtu.bbs.common.response.ApiResponse;
import edu.xtu.bbs.common.response.ResponseCode;
import edu.xtu.bbs.user.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Global Exception Handler Test
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should correctly handle business exceptions")
    void shouldHandleBusinessException() {
        // Given
        BusinessException exception = new BusinessException(ResponseCode.USER_NOT_FOUND, "Test user not found");

        // When
        ResponseEntity<ApiResponse<Object>> response = handler.handleBusinessException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(20001);
        assertThat(body.getMessage()).isEqualTo("Test user not found");
    }

    @Test
    @DisplayName("Should correctly handle user not found exceptions")
    void shouldHandleUserNotFoundException() {
        // Given
        UserNotFoundException exception = new UserNotFoundException("testUser", "Test user not found");

        // When
        ResponseEntity<ApiResponse<Object>> response = handler.handleUserNotFoundException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(20001);
        assertThat(body.getMessage()).isEqualTo("User not found");
    }


    @Test
    @DisplayName("Should correctly handle general exceptions")
    void shouldHandleGeneralException() {
        // Given
        Exception exception = new RuntimeException("Unknown error");

        // When
        ResponseEntity<ApiResponse<Object>> response = handler.handleGeneralException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiResponse<Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(90001);
        assertThat(body.getMessage()).isEqualTo("Internal server error");
    }

    @Test
    @DisplayName("Should correctly handle security exceptions")
    void shouldHandleSecurityException() {
        // Given
        SecurityException exception = new SecurityException("Insufficient permissions");

        // When
        ResponseEntity<ApiResponse<Object>> response = handler.handleSecurityException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ApiResponse<Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(10003);
        assertThat(body.getMessage()).isEqualTo("Insufficient permissions");
    }
}