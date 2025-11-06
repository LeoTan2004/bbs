package edu.xtu.bbs.user.filter;

import edu.xtu.bbs.user.service.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

public abstract class AbstractJwtAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    protected final JwtTokenService jwtTokenService;


    protected AbstractJwtAuthenticationFilter(RequestMatcher requiresAuthenticationRequestMatcher, AuthenticationManager authenticationManager, JwtTokenService jwtTokenService) {
        super(requiresAuthenticationRequestMatcher, authenticationManager);
        this.jwtTokenService = jwtTokenService;
    }

    protected static String obtainParameter(HttpServletRequest request, String key) {
        String parameter = request.getParameter(key);
        if (parameter == null) {
            parameter = "";
        }
        return parameter.trim();
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        final AbstractAuthenticationToken authRequest = getAuthenticationToken(request);
        authRequest.setDetails(request);
        return this.getAuthenticationManager().authenticate(authRequest);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) {
        final String username = getUsername(authResult);
        final String jwt = jwtTokenService.encode(username);
        response.addHeader("Authorization", "Bearer " + jwt);
    }

    protected abstract String getUsername(Authentication authResult);

    protected abstract AbstractAuthenticationToken getAuthenticationToken(HttpServletRequest request);
}
