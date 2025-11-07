package edu.xtu.bbs.user.filter;

import edu.xtu.bbs.user.service.JwtTokenService;
import edu.xtu.bbs.user.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * Simplified practical tests for JWT authorization filter
 * Focus on core functionality validation while avoiding complex type issues
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthorizationFilterTest {

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

    /**
     * Test that filter skips authentication paths
     */
    @Test
    void shouldSkipAuthPaths() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenService);
    }

    /**
     * Test handling when there is no Authorization header
     */
    @Test
    void shouldContinueWhenNoAuthHeader() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/user/profile");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Test handling of non-Bearer tokens
     */
    @Test
    void shouldIgnoreNonBearerToken() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/user/profile");
        when(request.getHeader("Authorization")).thenReturn("Basic dGVzdA==");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenService);
    }

    /**
     * Test processing of valid JWT tokens
     */
    @Test
    void shouldProcessValidJwtToken() throws Exception {
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
    }

    /**
     * Test handling of JWT parsing exceptions
     */
    @Test
    void shouldHandleJwtException() throws Exception {
        String invalidToken = "invalid.token";

        when(request.getRequestURI()).thenReturn("/api/user/profile");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(jwtTokenService.decode(invalidToken)).thenThrow(new RuntimeException("Invalid JWT"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Test behavior when authentication already exists
     * Note: Filter will still parse JWT, but won't override existing authentication
     */
    @Test
    void shouldNotOverrideExistingAuth() throws Exception {
        Authentication existing = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existing);

        String token = "some.token";
        when(request.getRequestURI()).thenReturn("/api/user/profile");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenService.decode(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn("testuser");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        // JWT will be parsed, but authentication won't be overridden
        verify(jwtTokenService).decode(token);
        assertEquals(existing, SecurityContextHolder.getContext().getAuthentication());
    }
}