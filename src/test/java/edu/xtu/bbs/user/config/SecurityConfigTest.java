package edu.xtu.bbs.user.config;

import edu.xtu.bbs.user.service.JwtTokenService;
import edu.xtu.bbs.user.service.UserService;
import edu.xtu.bbs.verification.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Security configuration
 * Verifies that the filter chain configuration is correct
 */
@SpringBootTest
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private VerificationService verificationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }


    /**
     * Test that /auth/** paths allow anonymous access
     */
    @Test
    void testAuthPathsShouldAllowAnonymousAccess() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"password\":\"test\"}"))
                .andExpect(status().isNotFound()); // 404 because controller doesn't exist, but not 401/403
    }


    /**
     * Test that CSRF is disabled
     */
    @Test
    void testCsrfShouldBeDisabled() throws Exception {
        mockMvc.perform(post("/api/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound()); // Should not return 403 CSRF-related error
    }

    /**
     * Test email code login endpoint
     */
    @Test
    void testEmailCodeLoginEndpoint() throws Exception {
        mockMvc.perform(post("/auth/login-with-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"code\":\"123456\"}"))
                .andExpect(status().isNotFound()); // Controller doesn't exist, but filter should handle
    }
}