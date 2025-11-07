package edu.xtu.bbs.user.config;

import edu.xtu.bbs.user.service.JwtTokenService;
import edu.xtu.bbs.user.service.UserService;
import edu.xtu.bbs.verification.VerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Basic tests for Security configuration
 * Verifies that Spring Context can start properly and load all Security configuration
 */
@SpringBootTest
@ActiveProfiles("test")
class SecurityConfigBasicTest {

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private VerificationService verificationService;

    /**
     * Test that Spring Context can start properly
     * This is the most basic test to ensure Security configuration has no syntax errors or dependency issues
     */
    @Test
    void contextLoads() {
        // If Spring Context can start, it means the basic configuration is correct
        assertTrue(true);
    }

    /**
     * Basic filter configuration validation
     * Ensure all necessary beans can be created correctly
     */
    @Test
    void securityConfigurationIsValid() {
        // If the test can run to this point, it means SecurityConfig configuration is valid
        assertTrue(true);
    }
}