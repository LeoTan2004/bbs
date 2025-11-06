package edu.xtu.bbs.user.filter;

import edu.xtu.bbs.user.service.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.stereotype.Component;

@Component
public class JwtUsernamePasswordAuthenticationFilter extends AbstractJwtAuthenticationFilter {


    public static final String SPRING_SECURITY_FORM_USERNAME_KEY = "username";
    public static final String SPRING_SECURITY_FORM_PASSWORD_KEY = "password";

    public static final String DEFAULT_LOGIN_URL = "/auth/login";
    public static final HttpMethod DEFAULT_LOGIN_METHOD = HttpMethod.POST;

    public JwtUsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager, JwtTokenService jwtTokenService) {
        super(PathPatternRequestMatcher.withDefaults().matcher(DEFAULT_LOGIN_METHOD, DEFAULT_LOGIN_URL), authenticationManager, jwtTokenService);
    }

    @Override
    protected String getUsername(Authentication authResult) {
        return authResult.getPrincipal().toString();
    }

    @Override
    protected AbstractAuthenticationToken getAuthenticationToken(HttpServletRequest request) {
        final String username = obtainParameter(request, SPRING_SECURITY_FORM_USERNAME_KEY);
        final String password = obtainParameter(request, SPRING_SECURITY_FORM_PASSWORD_KEY);
        return new UsernamePasswordAuthenticationToken(username, password);
    }

}
