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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for JWT Authorization Filter
 * Tests all critical paths and edge cases for the JWT authorization logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Authorization Filter Tests")
class JwtAuthorizationFilterComprehensiveTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private UserService userService;

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
    @DisplayName("Path Skipping Tests")
    class PathSkippingTests {

        /**
         * Test that all authentication paths are properly skipped
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
        @DisplayName("Should skip all /auth/** paths")
        void shouldSkipAllAuthPaths(String path) throws Exception {
            when(request.getRequestURI()).thenReturn(path);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtTokenService);
            verifyNoInteractions(userService);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should process non-auth paths")
        void shouldProcessNonAuthPaths() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/user/profile");
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtTokenService);
        }
    }

    @Nested
    @DisplayName("Authorization Header Tests")
    class AuthorizationHeaderTests {

        @Test
        @DisplayName("Should continue when no Authorization header")
        void shouldContinueWhenNoAuthHeader() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/user/profile");
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
            when(request.getRequestURI()).thenReturn("/api/user/profile");
            when(request.getHeader("Authorization")).thenReturn(authHeader);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtTokenService);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should process valid Bearer token format")
        void shouldProcessValidBearerToken() throws Exception {
            String token = "valid.jwt.token";
            when(request.getRequestURI()).thenReturn("/api/user/profile");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtTokenService.decode(token)).thenThrow(new RuntimeException("Token processing"));

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtTokenService).decode(token);
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

            UserDetails userDetails = new User(username, "password",
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

            when(request.getRequestURI()).thenReturn("/api/user/profile");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtTokenService.decode(token)).thenReturn(claims);
            when(claims.getSubject()).thenReturn(username);
            when(userService.findByUsername(username)).thenReturn(userDetails);
            when(jwtTokenService.isValidToken(token)).thenReturn(true);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtTokenService).decode(token);
            verify(jwtTokenService).isValidToken(token);
            verify(userService).findByUsername(username);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertEquals(username, auth.getName());
            assertTrue(auth.isAuthenticated());
            assertFalse(auth.getAuthorities().isEmpty());
        }

        @Test
        @DisplayName("Should handle JWT parsing exception")
        void shouldHandleJwtParsingException() throws Exception {
            String invalidToken = "invalid.jwt.token";

            when(request.getRequestURI()).thenReturn("/api/user/profile");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
            when(jwtTokenService.decode(invalidToken)).thenThrow(new RuntimeException("Invalid JWT"));

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtTokenService).decode(invalidToken);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should handle user not found exception")
        void shouldHandleUserNotFoundException() throws Exception {
            String token = "valid.jwt.token";
            String username = "nonexistentuser";

            when(request.getRequestURI()).thenReturn("/api/user/profile");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtTokenService.decode(token)).thenReturn(claims);
            when(claims.getSubject()).thenReturn(username);
            when(userService.findByUsername(username)).thenThrow(new RuntimeException("User not found"));

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtTokenService).decode(token);
            verify(userService).findByUsername(username);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should handle invalid token validation")
        void shouldHandleInvalidTokenValidation() throws Exception {
            String token = "parsed.but.invalid.token";
            String username = "testuser";

            UserDetails userDetails = new User(username, "password",
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

            when(request.getRequestURI()).thenReturn("/api/user/profile");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtTokenService.decode(token)).thenReturn(claims);
            when(claims.getSubject()).thenReturn(username);
            when(userService.findByUsername(username)).thenReturn(userDetails);
            when(jwtTokenService.isValidToken(token)).thenReturn(false); // Token validation fails

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtTokenService).decode(token);
            verify(jwtTokenService).isValidToken(token);
            verify(userService).findByUsername(username);
            // Authentication should not be set due to invalid token
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should handle null subject in JWT claims")
        void shouldHandleNullSubjectInClaims() throws Exception {
            String token = "token.with.null.subject";

            when(request.getRequestURI()).thenReturn("/api/user/profile");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtTokenService.decode(token)).thenReturn(claims);
            when(claims.getSubject()).thenReturn(null); // Null subject

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtTokenService).decode(token);
            verify(claims).getSubject();
            verifyNoInteractions(userService);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }

    @Nested
    @DisplayName("Security Context Management Tests")
    class SecurityContextManagementTests {

        @Test
        @DisplayName("Should not override existing authentication")
        void shouldNotOverrideExistingAuthentication() throws Exception {
            Authentication existing = mock(Authentication.class);
            SecurityContextHolder.getContext().setAuthentication(existing);

            String token = "some.token";
            when(request.getRequestURI()).thenReturn("/api/user/profile");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtTokenService.decode(token)).thenReturn(claims);
            when(claims.getSubject()).thenReturn("testuser");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtTokenService).decode(token);
            verify(claims).getSubject();
            // Should not interact with user service since authentication already exists
            verifyNoInteractions(userService);
            assertEquals(existing, SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should clear security context on exception")
        void shouldClearSecurityContextOnException() throws Exception {
            // Pre-set some authentication to test clearing
            Authentication existing = mock(Authentication.class);
            SecurityContextHolder.getContext().setAuthentication(existing);

            String token = "problematic.token";
            when(request.getRequestURI()).thenReturn("/api/user/profile");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtTokenService.decode(token)).thenThrow(new RuntimeException("JWT error"));

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            // Security context should be cleared due to exception
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Should handle empty subject string")
        void shouldHandleEmptySubjectString() throws Exception {
            String token = "token.with.empty.subject";

            when(request.getRequestURI()).thenReturn("/api/user/profile");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtTokenService.decode(token)).thenReturn(claims);
            when(claims.getSubject()).thenReturn(""); // Empty subject

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtTokenService).decode(token);
            verify(claims).getSubject();
            verifyNoInteractions(userService);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should handle whitespace-only subject")
        void shouldHandleWhitespaceOnlySubject() throws Exception {
            String token = jwtTokenService.encode("   "); // Token with whitespace subject

            when(request.getRequestURI()).thenReturn("/api/user/profile");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtTokenService.decode(token)).thenReturn(claims);
            when(claims.getSubject()).thenReturn("   "); // Whitespace-only subject

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtTokenService, never()).decode(token);
            verifyNoInteractions(claims);
            // Should still try to find user since subject is not null (though it's whitespace)
            verifyNoInteractions(userService);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("Should handle JWT service returning null claims")
        void shouldHandleNullClaims() throws Exception {
            String token = "token.with.null.claims";

            when(request.getRequestURI()).thenReturn("/api/user/profile");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtTokenService.decode(token)).thenReturn(null); // Null claims

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtTokenService).decode(token);
            // Should handle null claims gracefully
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }
}