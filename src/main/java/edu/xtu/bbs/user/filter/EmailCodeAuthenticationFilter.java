package edu.xtu.bbs.user.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.stereotype.Component;

/**
 * 邮箱验证码登录的认证过滤器
 */
@Component
public class EmailCodeAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public static final String SPRING_SECURITY_FORM_EMAIL_KEY = "principle";
    public static final String SPRING_SECURITY_FORM_CODE_KEY = "credential";
    public static final String SPRING_SECURITY_FORM_TOKEN_KEY = "token";
    public static final String SPRING_SECURITY_FORM_SCOPE_KEY = "scope";

    public EmailCodeAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/auth/login-with-code"), authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        final String email = obtainParameter(request, SPRING_SECURITY_FORM_EMAIL_KEY);
        final String code = obtainParameter(request, SPRING_SECURITY_FORM_CODE_KEY);
        final String token = obtainParameter(request, SPRING_SECURITY_FORM_TOKEN_KEY);
        final String scope = obtainParameter(request, SPRING_SECURITY_FORM_SCOPE_KEY);

        EmailCodeAuthenticationToken authRequest = new EmailCodeAuthenticationToken(email, token, scope, code);

        setDetails(request, authRequest);

        return this.getAuthenticationManager().authenticate(authRequest);
    }

    protected String obtainParameter(HttpServletRequest request, String key) {
        String parameter = request.getParameter(key);
        if (parameter == null) {
            parameter = "";
        }
        return parameter.trim();
    }


    protected void setDetails(HttpServletRequest request, EmailCodeAuthenticationToken authRequest) {
        authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
    }
}