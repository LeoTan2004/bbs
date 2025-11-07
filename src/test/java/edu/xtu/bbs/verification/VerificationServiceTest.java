package edu.xtu.bbs.verification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    private final String testPrincipal = "test@example.com";
    private final String testAction = "register";
    private final String testCode = "123456";
    private final String testToken = "test-token-123";
    @Mock
    private VerificationSender verificationSender;
    @Mock
    private VerificationRequestRepository verificationRequestRepository;
    @InjectMocks
    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        // Set configuration properties
        ReflectionTestUtils.setField(verificationService, "codeLength", 6);
        ReflectionTestUtils.setField(verificationService, "validDuration", Duration.ofMinutes(3));
        ReflectionTestUtils.setField(verificationService, "verifyDuration", Duration.ofSeconds(1));
        ReflectionTestUtils.setField(verificationService, "maxValidCount", 5);
    }

    @Test
    @DisplayName("Should generate a 6-digit numeric code")
    void testGenerateCode() {
        // Test protected method using reflection
        String code = ReflectionTestUtils.invokeMethod(verificationService, "generateCode");

        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    @DisplayName("Should generate unique UUID tokens")
    void testGenerateToken() {
        // Test protected method using reflection
        String token1 = ReflectionTestUtils.invokeMethod(verificationService, "generateToken");
        String token2 = ReflectionTestUtils.invokeMethod(verificationService, "generateToken");

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        // UUID format validation
        assertDoesNotThrow(() -> UUID.fromString(token1));
        assertDoesNotThrow(() -> UUID.fromString(token2));
    }

    @Test
    @DisplayName("Should send code successfully and return token")
    void testSendCode_Success() {
        // Arrange
        when(verificationSender.sendCode(eq(testPrincipal), anyString())).thenReturn(true);

        VerificationRequest savedRequest = createTestVerificationRequest();
        savedRequest.setToken(testToken);
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenReturn(savedRequest);

        // Act
        String result = verificationService.sendCode(testPrincipal, testAction);

        // Assert
        assertNotNull(result);
        assertEquals(testToken, result);

        verify(verificationSender).sendCode(eq(testPrincipal), anyString());
        verify(verificationRequestRepository).save(argThat(request ->
                testPrincipal.equals(request.getPrinciple()) &&
                        testAction.equals(request.getScope()) &&
                        request.getCredential() != null &&
                        request.getToken() != null &&
                        request.getRequestedAt() != null &&
                        request.getExpiresAt() != null &&
                        request.getValidCount() == 0 &&
                        request.getStatus() == VerificationStatus.Pending
        ));
    }

    @Test
    @DisplayName("Should return empty string when send fails")
    void testSendCode_SendFailure() {
        // Arrange
        when(verificationSender.sendCode(eq(testPrincipal), anyString())).thenReturn(false);

        // Act
        String result = verificationService.sendCode(testPrincipal, testAction);

        // Assert
        assertEquals("", result);

        verify(verificationSender).sendCode(eq(testPrincipal), anyString());
        verify(verificationRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should verify code successfully with correct credentials")
    void testVerifyCode_Success() throws Exception {
        // Arrange
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(0);
        request.setLastValidAt(null);

        VerificationParam param = new VerificationParam(testPrincipal, testToken, testAction, testCode);

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenReturn(request);

        // Act
        boolean result = verificationService.verifyCode(param);

        // Assert
        assertTrue(result);
        assertEquals(VerificationStatus.Verified, request.getStatus());
        assertEquals(1, request.getValidCount());
        assertNotNull(request.getLastValidAt());

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository).save(request);
    }

    @Test
    @DisplayName("Should fail verification with wrong code")
    void testVerifyCode_WrongCode() throws Exception {
        // Arrange
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(0);
        request.setLastValidAt(null);

        VerificationParam param = new VerificationParam(testPrincipal, testToken, testAction, "wrong-code");

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenReturn(request);

        // Act
        boolean result = verificationService.verifyCode(param);

        // Assert
        assertFalse(result);
        assertEquals(VerificationStatus.Pending, request.getStatus());
        assertEquals(1, request.getValidCount());
        assertNotNull(request.getLastValidAt());

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository).save(request);
    }

    @Test
    @DisplayName("Should throw exception when verification request not found")
    void testVerifyCode_RequestNotFound() {
        // Arrange
        VerificationParam param = new VerificationParam(testPrincipal, testToken, testAction, testCode);
        
        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(VerificationRequestNotFoundException.class, () ->
                verificationService.verifyCode(param)
        );

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when verification code is expired")
    void testVerifyCode_Expired() {
        // Arrange
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().minusSeconds(10)); // Expired
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(0);

        VerificationParam param = new VerificationParam(testPrincipal, testToken, testAction, testCode);

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));

        // Act & Assert
        assertThrows(VerificationExpiredException.class, () ->
                verificationService.verifyCode(param)
        );

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when request is already verified")
    void testVerifyCode_AlreadyVerified() {
        // Arrange
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Verified); // Already verified
        request.setValidCount(0);

        VerificationParam param = new VerificationParam(testPrincipal, testToken, testAction, testCode);

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));

        // Act & Assert
        assertThrows(VerificationExpiredException.class, () ->
                verificationService.verifyCode(param)
        );

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when verification attempts are too frequent by time")
    void testVerifyCode_TooFrequent_ByTime() {
        // Arrange
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(0);
        request.setLastValidAt(Instant.now()); // Just verified

        VerificationParam param = new VerificationParam(testPrincipal, testToken, testAction, testCode);

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));

        // Act & Assert
        assertThrows(VerificationTooFrequentException.class, () ->
                verificationService.verifyCode(param)
        );

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when verification attempts reach maximum count")
    void testVerifyCode_TooFrequent_ByCount() {
        // Arrange
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(5); // Reached maximum count
        request.setLastValidAt(Instant.now().minusSeconds(2)); // Time has passed

        VerificationParam param = new VerificationParam(testPrincipal, testToken, testAction, testCode);

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));

        // Act & Assert
        assertThrows(VerificationTooFrequentException.class, () ->
                verificationService.verifyCode(param)
        );

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle null valid count properly")
    void testVerifyCode_NullValidCount() throws Exception {
        // Arrange - Test when validCount is null
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(null); // null value
        request.setLastValidAt(null);

        VerificationParam param = new VerificationParam(testPrincipal, testToken, testAction, testCode);

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenReturn(request);

        // Act
        boolean result = verificationService.verifyCode(param);

        // Assert
        assertTrue(result);
        assertEquals(VerificationStatus.Verified, request.getStatus());
        assertEquals(1, request.getValidCount()); // Changed from null to 1
        assertNotNull(request.getLastValidAt());

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository).save(request);
    }

    @Test
    @DisplayName("Should handle null last valid time properly")
    void testVerifyCode_NullLastValidAt() throws Exception {
        // Arrange - Test when lastValidAt is null (first verification)
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(2);
        request.setLastValidAt(null); // null value

        VerificationParam param = new VerificationParam(testPrincipal, testToken, testAction, testCode);

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenReturn(request);

        // Act
        boolean result = verificationService.verifyCode(param);

        // Assert
        assertTrue(result);
        assertEquals(VerificationStatus.Verified, request.getStatus());
        assertEquals(3, request.getValidCount());
        assertNotNull(request.getLastValidAt());

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository).save(request);
    }

    // Additional scope validation test methods

    @Test
    @DisplayName("Should fail verification when scope does not match")
    void testVerifyCode_WrongScope() throws Exception {
        // Arrange - Test when scope does not match
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(0);
        request.setLastValidAt(null);
        request.setScope("register"); // Set to register

        // Create a param with mismatched scope (pass login)
        VerificationParam param = new VerificationParam(testPrincipal, testToken, "login", testCode);

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenReturn(request);

        // Act
        boolean result = verificationService.verifyCode(param);

        // Assert
        assertFalse(result); // Scope mismatch, verification should fail
        assertEquals(VerificationStatus.Pending, request.getStatus()); // Status should remain Pending
        assertEquals(1, request.getValidCount()); // Validation count should increase
        assertNotNull(request.getLastValidAt()); // Last validation time should be updated

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository).save(request);
    }

    @Test
    @DisplayName("Should succeed verification when scope matches")
    void testVerifyCode_CorrectScope() throws Exception {
        // Arrange - Test when scope matches
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(0);
        request.setLastValidAt(null);
        request.setScope("register"); // Set to register

        // Create a param with matching scope
        VerificationParam param = new VerificationParam(testPrincipal, testToken, "register", testCode);

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenReturn(request);

        // Act
        boolean result = verificationService.verifyCode(param);

        // Assert
        assertTrue(result); // Scope matches and credential correct, verification should succeed
        assertEquals(VerificationStatus.Verified, request.getStatus()); // Status should change to Verified
        assertEquals(1, request.getValidCount()); // Validation count should increase
        assertNotNull(request.getLastValidAt()); // Last validation time should be updated

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository).save(request);
    }

    @Test
    @DisplayName("Should fail verification when request scope is null")
    void testVerifyCode_NullScope() throws Exception {
        // Arrange - Test when request scope is null
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(0);
        request.setLastValidAt(null);
        request.setScope(null); // Set to null

        VerificationParam param = new VerificationParam(testPrincipal, testToken, "register", testCode);

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenReturn(request);

        // Act
        boolean result = verificationService.verifyCode(param);

        // Assert
        assertFalse(result); // Scope mismatch (null != "register"), verification should fail
        assertEquals(VerificationStatus.Pending, request.getStatus());
        assertEquals(1, request.getValidCount());
        assertNotNull(request.getLastValidAt());

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository).save(request);
    }

    @Test
    @DisplayName("Should succeed verification when both scopes are empty")
    void testVerifyCode_EmptyScope() throws Exception {
        // Arrange - Test empty string scope
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(0);
        request.setLastValidAt(null);
        request.setScope(""); // Set to empty string

        VerificationParam param = new VerificationParam(testPrincipal, testToken, "", testCode);

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenReturn(request);

        // Act
        boolean result = verificationService.verifyCode(param);

        // Assert
        assertTrue(result); // Both are empty strings, should match
        assertEquals(VerificationStatus.Verified, request.getStatus());
        assertEquals(1, request.getValidCount());
        assertNotNull(request.getLastValidAt());

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository).save(request);
    }

    @Test
    @DisplayName("Should fail verification when credentials are correct but scope is wrong")
    void testVerifyCode_CorrectCredentialWrongScope() throws Exception {
        // Arrange - Test correct credentials but wrong scope
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode); // Correct credentials
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(0);
        request.setLastValidAt(null);
        request.setScope("password-reset"); // Set to password-reset

        // Pass correct credentials but wrong scope
        VerificationParam param = new VerificationParam(testPrincipal, testToken, "email-verification", testCode);

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenReturn(request);

        // Act
        boolean result = verificationService.verifyCode(param);

        // Assert
        assertFalse(result); // Even with correct credentials, scope mismatch should fail
        assertEquals(VerificationStatus.Pending, request.getStatus());
        assertEquals(1, request.getValidCount());
        assertNotNull(request.getLastValidAt());

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository).save(request);
    }

    @Test
    @DisplayName("Should fail verification when credentials are wrong but scope is correct")
    void testVerifyCode_WrongCredentialCorrectScope() throws Exception {
        // Arrange - Test wrong credentials but correct scope
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode); // Correct credentials
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(0);
        request.setLastValidAt(null);
        request.setScope("register"); // Set to register

        // Pass wrong credentials but correct scope
        VerificationParam param = new VerificationParam(testPrincipal, testToken, "register", "wrong-code");

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenReturn(request);

        // Act
        boolean result = verificationService.verifyCode(param);

        // Assert
        assertFalse(result); // Even with correct scope, wrong credentials should fail
        assertEquals(VerificationStatus.Pending, request.getStatus());
        assertEquals(1, request.getValidCount());
        assertNotNull(request.getLastValidAt());

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository).save(request);
    }

    private VerificationRequest createTestVerificationRequest() {
        VerificationRequest request = new VerificationRequest();
        request.setId(1);
        request.setPrinciple(testPrincipal);
        request.setScope(testAction);
        request.setToken(testToken);
        request.setCredential(testCode);
        request.setRequestedAt(Instant.now());
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(0);
        return request;
    }
}