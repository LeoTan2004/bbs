package edu.xtu.bbs.verification;

import org.junit.jupiter.api.BeforeEach;
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
        // 设置配置属性
        ReflectionTestUtils.setField(verificationService, "codeLength", 6);
        ReflectionTestUtils.setField(verificationService, "validDuration", Duration.ofMinutes(3));
        ReflectionTestUtils.setField(verificationService, "verifyDuration", Duration.ofSeconds(1));
        ReflectionTestUtils.setField(verificationService, "maxValidCount", 5);
    }

    @Test
    void testGenerateCode() {
        // 使用反射测试protected方法
        String code = ReflectionTestUtils.invokeMethod(verificationService, "generateCode");

        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    void testGenerateToken() {
        // 使用反射测试protected方法
        String token1 = ReflectionTestUtils.invokeMethod(verificationService, "generateToken");
        String token2 = ReflectionTestUtils.invokeMethod(verificationService, "generateToken");

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        // UUID格式验证
        assertDoesNotThrow(() -> UUID.fromString(token1));
        assertDoesNotThrow(() -> UUID.fromString(token2));
    }

    @Test
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
    void testVerifyCode_Expired() {
        // Arrange
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().minusSeconds(10)); // 过期
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
    void testVerifyCode_AlreadyVerified() {
        // Arrange
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Verified); // 已验证
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
    void testVerifyCode_TooFrequent_ByTime() {
        // Arrange
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(0);
        request.setLastValidAt(Instant.now()); // 刚刚验证过

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
    void testVerifyCode_TooFrequent_ByCount() {
        // Arrange
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(5); // 达到最大次数
        request.setLastValidAt(Instant.now().minusSeconds(2)); // 时间已过

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
    void testVerifyCode_NullValidCount() throws Exception {
        // Arrange - 测试 validCount 为 null 的情况
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(null); // null 值
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
        assertEquals(1, request.getValidCount()); // 从 null 变为 1
        assertNotNull(request.getLastValidAt());

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository).save(request);
    }

    @Test
    void testVerifyCode_NullLastValidAt() throws Exception {
        // Arrange - 测试 lastValidAt 为 null 的情况（初次验证）
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(2);
        request.setLastValidAt(null); // null 值

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

    // 新增：测试 scope 校验相关的测试方法

    @Test
    void testVerifyCode_WrongScope() throws Exception {
        // Arrange - 测试 scope 不匹配的情况
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(0);
        request.setLastValidAt(null);
        request.setScope("register"); // 设置为 register

        // 创建一个 scope 不匹配的参数（传入 login）
        VerificationParam param = new VerificationParam(testPrincipal, testToken, "login", testCode);

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenReturn(request);

        // Act
        boolean result = verificationService.verifyCode(param);

        // Assert
        assertFalse(result); // scope 不匹配，验证应该失败
        assertEquals(VerificationStatus.Pending, request.getStatus()); // 状态应该保持为 Pending
        assertEquals(1, request.getValidCount()); // 验证次数应该增加
        assertNotNull(request.getLastValidAt()); // 最后验证时间应该被更新

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository).save(request);
    }

    @Test
    void testVerifyCode_CorrectScope() throws Exception {
        // Arrange - 测试 scope 匹配的情况
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(0);
        request.setLastValidAt(null);
        request.setScope("register"); // 设置为 register

        // 创建一个 scope 匹配的参数
        VerificationParam param = new VerificationParam(testPrincipal, testToken, "register", testCode);

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenReturn(request);

        // Act
        boolean result = verificationService.verifyCode(param);

        // Assert
        assertTrue(result); // scope 匹配且 credential 正确，验证应该成功
        assertEquals(VerificationStatus.Verified, request.getStatus()); // 状态应该变为 Verified
        assertEquals(1, request.getValidCount()); // 验证次数应该增加
        assertNotNull(request.getLastValidAt()); // 最后验证时间应该被更新

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository).save(request);
    }

    @Test
    void testVerifyCode_NullScope() throws Exception {
        // Arrange - 测试 request 中 scope 为 null 的情况
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(0);
        request.setLastValidAt(null);
        request.setScope(null); // 设置为 null

        VerificationParam param = new VerificationParam(testPrincipal, testToken, "register", testCode);

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenReturn(request);

        // Act
        boolean result = verificationService.verifyCode(param);

        // Assert
        assertFalse(result); // scope 不匹配（null != "register"），验证应该失败
        assertEquals(VerificationStatus.Pending, request.getStatus());
        assertEquals(1, request.getValidCount());
        assertNotNull(request.getLastValidAt());

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository).save(request);
    }

    @Test
    void testVerifyCode_EmptyScope() throws Exception {
        // Arrange - 测试空字符串 scope 的情况
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode);
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(0);
        request.setLastValidAt(null);
        request.setScope(""); // 设置为空字符串

        VerificationParam param = new VerificationParam(testPrincipal, testToken, "", testCode);

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenReturn(request);

        // Act
        boolean result = verificationService.verifyCode(param);

        // Assert
        assertTrue(result); // 两个都是空字符串，应该匹配
        assertEquals(VerificationStatus.Verified, request.getStatus());
        assertEquals(1, request.getValidCount());
        assertNotNull(request.getLastValidAt());

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository).save(request);
    }

    @Test
    void testVerifyCode_CorrectCredentialWrongScope() throws Exception {
        // Arrange - 测试凭据正确但 scope 错误的情况
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode); // 正确的凭据
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(0);
        request.setLastValidAt(null);
        request.setScope("password-reset"); // 设置为 password-reset

        // 传入正确的凭据但错误的 scope
        VerificationParam param = new VerificationParam(testPrincipal, testToken, "email-verification", testCode);

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenReturn(request);

        // Act
        boolean result = verificationService.verifyCode(param);

        // Assert
        assertFalse(result); // 即使凭据正确，scope 不匹配也应该失败
        assertEquals(VerificationStatus.Pending, request.getStatus());
        assertEquals(1, request.getValidCount());
        assertNotNull(request.getLastValidAt());

        verify(verificationRequestRepository).findByPrincipleAndToken(testPrincipal, testToken);
        verify(verificationRequestRepository).save(request);
    }

    @Test
    void testVerifyCode_WrongCredentialCorrectScope() throws Exception {
        // Arrange - 测试凭据错误但 scope 正确的情况
        VerificationRequest request = createTestVerificationRequest();
        request.setCredential(testCode); // 正确的凭据
        request.setExpiresAt(Instant.now().plusSeconds(300));
        request.setStatus(VerificationStatus.Pending);
        request.setValidCount(0);
        request.setLastValidAt(null);
        request.setScope("register"); // 设置为 register

        // 传入错误的凭据但正确的 scope
        VerificationParam param = new VerificationParam(testPrincipal, testToken, "register", "wrong-code");

        when(verificationRequestRepository.findByPrincipleAndToken(testPrincipal, testToken))
                .thenReturn(Optional.of(request));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenReturn(request);

        // Act
        boolean result = verificationService.verifyCode(param);

        // Assert
        assertFalse(result); // 即使 scope 正确，凭据错误也应该失败
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