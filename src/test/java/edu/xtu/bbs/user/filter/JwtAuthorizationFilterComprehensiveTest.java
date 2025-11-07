package edu.xtu.bbs.user.filter;

import edu.xtu.bbs.user.service.JwtTokenService;
import edu.xtu.bbs.user.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.PathMatcher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for JWT Authorization Filter
 * Tests all critical paths and edge cases for the JWT authorization logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Authorization Filter Comprehensive Tests")
class JwtAuthorizationFilterComprehensiveTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private UserService userService;

    @Mock
    private PathMatcher pathMatcher;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Claims claims;

    @InjectMocks
    private JwtAuthorizationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Path Filtering Tests")
    class PathFilteringTests {

        /**
         * Test that shouldNotFilter method correctly identifies auth paths
         */
        @ParameterizedTest
        @ValueSource(strings = {
                "/auth/login",
                "/auth/register",
                "/auth/login-with-code",
                "/auth/forgot-password",
                "/auth/reset-password",
                "/auth/verify-email"
        })
        @DisplayName("Should not filter /auth/** paths")
        void shouldNotFilterAuthPaths(String path) {
            when(request.getServletPath()).thenReturn(path);
            when(pathMatcher.match("/auth/**", path)).thenReturn(true);

            assertTrue(filter.shouldNotFilter(request));
        }

        @Test
        @DisplayName("Should filter non-auth paths")
        void shouldFilterNonAuthPaths() {
            String path = "/api/user/profile";
            when(request.getServletPath()).thenReturn(path);
            when(pathMatcher.match("/auth/**", path)).thenReturn(false);

            assertFalse(filter.shouldNotFilter(request));
        }
    }

    @Nested
    @DisplayName("Authorization Header Tests")
    class AuthorizationHeaderTests {

        @Test
        @DisplayName("Should continue when no Authorization header")
        void shouldContinueWhenNoAuthHeader() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtTokenService);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Basic dGVzdA==",
                "Digest username=\"test\"",
                "ApiKey 123456",
                "Bearer", // Invalid - no token
                "", // Empty
                "InvalidFormat token123"
        })
        @DisplayName("Should ignore non-Bearer or invalid authorization headers")
        void shouldIgnoreInvalidAuthHeaders(String authHeader) throws Exception {
            when(request.getHeader("Authorization")).thenReturn(authHeader);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtTokenService);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }

    @Nested
    @DisplayName("JWT Token Processing Tests")
    class JwtTokenProcessingTests {

        @Test
        @DisplayName("Should successfully authenticate valid JWT token")
        void shouldAuthenticateValidJwtToken() throws Exception {
            String token = "valid.jwt.token";
            String username = "testuser";

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtTokenService.decode(token)).thenReturn(claims);
            when(jwtTokenService.isValidToken(claims)).thenReturn(true);
            when(claims.getSubject()).thenReturn(username);
            when(userService.existsByUsername(username)).thenReturn(true);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtTokenService).decode(token);
            verify(jwtTokenService).isValidToken(claims);
            verify(userService).existsByUsername(username);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertEquals(username, auth.getPrincipal());
            assertNull(auth.getCredentials());
            // The authorities will be an empty collection, not null
            assertTrue(auth.getAuthorities() == null || auth.getAuthorities().isEmpty());
        }

        @Test
        @DisplayName("Should return 401 when token is invalid")
        void shouldReturn401WhenTokenInvalid() throws Exception {
            String token = "invalid.token";

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtTokenService.decode(token)).thenReturn(claims);
            when(jwtTokenService.isValidToken(claims)).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
            verify(jwtTokenService).decode(token);
            verify(jwtTokenService).isValidToken(claims);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should return 401 when user not found")
        void shouldReturn401WhenUserNotFound() throws Exception {
            String token = "valid.jwt.token";
            String username = "nonexistentuser";

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtTokenService.decode(token)).thenReturn(claims);
            when(jwtTokenService.isValidToken(claims)).thenReturn(true);
            when(claims.getSubject()).thenReturn(username);
            when(userService.existsByUsername(username)).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
            verify(jwtTokenService).decode(token);
            verify(jwtTokenService).isValidToken(claims);
            verify(userService).existsByUsername(username);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should handle JWT parsing exception and continue")
        void shouldHandleJwtParsingException() throws Exception {
            String invalidToken = "invalid.jwt.token";

            when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
            when(jwtTokenService.decode(invalidToken)).thenThrow(new RuntimeException("Invalid JWT"));

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtTokenService).decode(invalidToken);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should continue when JWT has empty or null username")
        void shouldContinueWhenEmptyUsername() throws Exception {
            String token = "token.with.empty.subject";

            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtTokenService.decode(token)).thenReturn(claims);
            when(jwtTokenService.isValidToken(claims)).thenReturn(true);
            when(claims.getSubject()).thenReturn("");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtTokenService).decode(token);
            verify(jwtTokenService).isValidToken(claims);
            verify(claims).getSubject();
            verifyNoInteractions(userService);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }

    @Nested
    @DisplayName("Security Context Management Tests")
    class SecurityContextManagementTests {

        @Test
        @DisplayName("Should not process JWT when user is already authenticated")
        void shouldNotProcessJwtWhenAlreadyAuthenticated() throws Exception {
            Authentication existing = mock(Authentication.class);
            SecurityContextHolder.getContext().setAuthentication(existing);

            String token = "some.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            // Should not interact with JWT service since authentication already exists
            verifyNoInteractions(jwtTokenService);
            verifyNoInteractions(userService);
            assertEquals(existing, SecurityContextHolder.getContext().getAuthentication());
        }
    }
}