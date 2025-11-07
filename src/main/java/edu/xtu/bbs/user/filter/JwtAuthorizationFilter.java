package edu.xtu.bbs.user.filter;

import edu.xtu.bbs.user.exception.JwtTokenInvalidationException;
import edu.xtu.bbs.user.service.JwtTokenService;
import edu.xtu.bbs.user.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserService userService;
    private final PathMatcher pathMatcher;


    public JwtAuthorizationFilter(JwtTokenService jwtTokenService, UserService userService, PathMatcher pathMatcher) {
        this.jwtTokenService = jwtTokenService;
        this.userService = userService;
        this.pathMatcher = pathMatcher;
    }


    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authorization = request.getHeader("Authorization");

        // If there is no Authorization header, or it is not a Bearer token, continue the filter chain
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        // If the user is already authenticated, continue the filter chain
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authorization.substring(7);
            final Claims claims = jwtTokenService.decode(jwt);
            if (!jwtTokenService.isValidToken(claims)) {
                throw new JwtTokenInvalidationException("Invalid token");
            }
            final String username = claims.getSubject();
            if (!StringUtils.hasText(username)) {
                filterChain.doFilter(request, response);
                return;
            }
            if (!userService.existsByUsername(username)) {
                throw new UsernameNotFoundException("User not found: " + username);
            }
            final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    null
            );
            // The token has been authenticated by JWT
            SecurityContextHolder.getContext().setAuthentication(token);

        } catch (UsernameNotFoundException | JwtTokenInvalidationException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (Exception e) {
            // On any exception (e.g., invalid token), clear the security context to let subsequent filters handle it
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return pathMatcher.match("/auth/**", request.getServletPath());
    }
}