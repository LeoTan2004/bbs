package edu.xtu.bbs.user.filter;

import edu.xtu.bbs.user.service.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.stereotype.Component;

/**
 * WeChat authentication filter for handling login with WeChat authorization code.
 */
@Component
public class JwtWeChatAppAuthenticationFilter extends AbstractJwtAuthenticationFilter {

    public static final String SPRING_SECURITY_FORM_CODE_KEY = "code";
    public static final String DEFAULT_LOGIN_URL = "/auth/login-with-wechat";
    public static final HttpMethod DEFAULT_LOGIN_METHOD = HttpMethod.POST;

    public JwtWeChatAppAuthenticationFilter(AuthenticationManager authenticationManager,
                                            JwtTokenService jwtTokenService) {
        super(PathPatternRequestMatcher.withDefaults().matcher(DEFAULT_LOGIN_METHOD, DEFAULT_LOGIN_URL),
                authenticationManager, jwtTokenService);
    }

    @Override
    protected String getUsername(Authentication authResult) {
        // Get username from the authentication result
        Object principal = authResult.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return principal.toString();
    }

    @Override
    protected AbstractAuthenticationToken getAuthenticationToken(HttpServletRequest request) {
        String code = obtainCode(request);
        if (code == null) {
            code = "";
        }
        code = code.trim();

        return new WeChatAppAuthenticationToken(code);
    }

    private String obtainCode(HttpServletRequest request) {
        return request.getParameter(SPRING_SECURITY_FORM_CODE_KEY);
    }
}
