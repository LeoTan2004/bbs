package edu.xtu.bbs.user.filter;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class WeChatAppAuthenticationToken extends AbstractAuthenticationToken {

    private final String code;
    private final Object principal;

    // Constructor for initial authentication with code
    public WeChatAppAuthenticationToken(String code) {
        super(null);
        this.code = code;
        this.principal = null;
        setAuthenticated(false);
    }

    // Constructor for successful authentication with user principal
    public WeChatAppAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.code = null;
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public void eraseCredentials() {
        // code is already handled by being final
    }

    @Override
    public Object getCredentials() {
        return code;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
