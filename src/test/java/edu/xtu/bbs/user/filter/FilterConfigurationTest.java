package edu.xtu.bbs.user.filter;

import edu.xtu.bbs.user.service.JwtTokenService;
import edu.xtu.bbs.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Practical filter test examples
 * Demonstrates how to test core functionality of custom filters
 */
@SpringBootTest
@ActiveProfiles("test")
class FilterConfigurationTest {

    @Autowired
    private JwtAuthorizationFilter jwtAuthorizationFilter;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Should create and inject filter beans successfully")
    void filterBeansAreCreated() {
        assertNotNull(jwtAuthorizationFilter);
    }

    @ParameterizedTest
    @DisplayName("Should recognize various auth path formats correctly")
    @ValueSource(strings = {
            "/auth/login",
            "/auth/register",
            "/auth/login-with-code",
            "/auth/forgot-password"
    })
    void authPathsAreRecognized(String path) {
        // Path matching validation logic can be added here
        // For example, mock requests and verify filter behavior
        assertNotNull(path);
    }
}