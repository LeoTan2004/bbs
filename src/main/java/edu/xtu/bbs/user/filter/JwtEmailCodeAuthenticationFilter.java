package edu.xtu.bbs.user.filter;

import edu.xtu.bbs.user.exception.EmailNotFoundException;
import edu.xtu.bbs.user.repo.UserRepository;
import edu.xtu.bbs.user.service.JwtTokenService;
import edu.xtu.bbs.user.service.UserBinderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.stereotype.Component;

/**
 * Email and code based authentication filter.
 */
@Component
public class JwtEmailCodeAuthenticationFilter extends AbstractJwtAuthenticationFilter {

    public static final String SPRING_SECURITY_FORM_EMAIL_KEY = "principle";
    public static final String SPRING_SECURITY_FORM_CODE_KEY = "credential";
    public static final String SPRING_SECURITY_FORM_TOKEN_KEY = "token";
    public static final String SPRING_SECURITY_FORM_SCOPE_KEY = "scope";

    public static final String DEFAULT_LOGIN_URL = "/auth/login-with-code";
    public static final HttpMethod DEFAULT_LOGIN_METHOD = HttpMethod.POST;

    private final UserRepository userRepository;
    private final UserBinderService userBinderService;


    public JwtEmailCodeAuthenticationFilter(AuthenticationManager authenticationManager, UserRepository userRepository, JwtTokenService jwtTokenService, UserBinderService userBinderService) {
        super(PathPatternRequestMatcher.withDefaults().matcher(DEFAULT_LOGIN_METHOD, DEFAULT_LOGIN_URL), authenticationManager, jwtTokenService);
        this.userRepository = userRepository;
        this.userBinderService = userBinderService;
    }


    @Override
    protected String getUsername(Authentication authResult) {
        final String email = authResult.getPrincipal().toString();
        return userBinderService.findUserIdByEmail(email)
                .flatMap(userRepository::findById)
                .orElseThrow(() -> new RuntimeException(new EmailNotFoundException(email, "could not find user with email")))
                .getUsername();
    }

    @Override
    protected AbstractAuthenticationToken getAuthenticationToken(HttpServletRequest request) {
        final String email = obtainParameter(request, SPRING_SECURITY_FORM_EMAIL_KEY);
        final String code = obtainParameter(request, SPRING_SECURITY_FORM_CODE_KEY);
        final String token = obtainParameter(request, SPRING_SECURITY_FORM_TOKEN_KEY);
        final String scope = obtainParameter(request, SPRING_SECURITY_FORM_SCOPE_KEY);

        return new EmailCodeAuthenticationToken(email, token, scope, code);
    }
}