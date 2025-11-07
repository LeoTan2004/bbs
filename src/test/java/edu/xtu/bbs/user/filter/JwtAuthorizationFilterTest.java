package edu.xtu.bbs.user.filter;

import edu.xtu.bbs.user.service.JwtTokenService;
import edu.xtu.bbs.user.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.PathMatcher;

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

    /**
     * Test shouldNotFilter method for authentication paths
     */
    @Test
    @DisplayName("Should not filter auth paths")
    void shouldNotFilterAuthPaths() {
        when(request.getServletPath()).thenReturn("/auth/login");
        when(pathMatcher.match("/auth/**", "/auth/login")).thenReturn(true);

        // The shouldNotFilter method should return true for auth paths
        assertTrue(filter.shouldNotFilter(request));
    }

    /**
     * Test shouldNotFilter method for non-auth paths
     */
    @Test
    @DisplayName("Should filter non-auth paths")
    void shouldFilterNonAuthPaths() {
        when(request.getServletPath()).thenReturn("/api/user/profile");
        when(pathMatcher.match("/auth/**", "/api/user/profile")).thenReturn(false);

        // The shouldNotFilter method should return false for non-auth paths
        assertFalse(filter.shouldNotFilter(request));
    }

    /**
     * Test handling when there is no Authorization header
     */
    @Test
    @DisplayName("Should continue processing when no Authorization header is present")
    void shouldContinueWhenNoAuthHeader() throws Exception {
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
    @DisplayName("Should ignore non-Bearer tokens")
    void shouldIgnoreNonBearerToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic dGVzdA==");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenService);
    }

    /**
     * Test behavior when user is already authenticated
     */
    @Test
    @DisplayName("Should continue when user is already authenticated")
    void shouldContinueWhenAlreadyAuthenticated() throws Exception {
        Authentication existing = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existing);

        when(request.getHeader("Authorization")).thenReturn("Bearer some.token");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenService);
        assertEquals(existing, SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Test processing of valid JWT tokens
     */
    @Test
    @DisplayName("Should process valid JWT token and set authentication")
    void shouldProcessValidJwtToken() throws Exception {
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

    /**
     * Test handling of invalid JWT tokens
     */
    @Test
    @DisplayName("Should return 401 for invalid JWT token")
    void shouldReturn401ForInvalidToken() throws Exception {
        String token = "invalid.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenService.decode(token)).thenReturn(claims);
        when(jwtTokenService.isValidToken(claims)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Test handling when user does not exist
     */
    @Test
    @DisplayName("Should return 401 when user does not exist")
    void shouldReturn401WhenUserNotExists() throws Exception {
        String token = "valid.token";
        String username = "nonexistentuser";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenService.decode(token)).thenReturn(claims);
        when(jwtTokenService.isValidToken(claims)).thenReturn(true);
        when(claims.getSubject()).thenReturn(username);
        when(userService.existsByUsername(username)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Test handling of empty username in JWT
     */
    @Test
    @DisplayName("Should continue when JWT has empty username")
    void shouldContinueWhenEmptyUsername() throws Exception {
        String token = "token.with.empty.subject";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtTokenService.decode(token)).thenReturn(claims);
        when(jwtTokenService.isValidToken(claims)).thenReturn(true);
        when(claims.getSubject()).thenReturn("");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Test handling of JWT parsing exceptions
     */
    @Test
    @DisplayName("Should handle JWT parsing exceptions gracefully")
    void shouldHandleJwtException() throws Exception {
        String invalidToken = "invalid.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(jwtTokenService.decode(invalidToken)).thenThrow(new RuntimeException("Invalid JWT"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}