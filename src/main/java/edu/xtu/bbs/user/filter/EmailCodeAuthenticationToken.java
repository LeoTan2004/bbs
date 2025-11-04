package edu.xtu.bbs.user.filter;

import edu.xtu.bbs.user.service.AuthenticationService;
import edu.xtu.bbs.verification.VerificationParam;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class EmailCodeAuthenticationToken extends AbstractAuthenticationToken {

    private final String principle;
    private final String token;
    private final String scope;
    private String credential;

    public EmailCodeAuthenticationToken(String principle, String token, String scope, String code) {
        super(null);
        this.principle = principle;
        this.token = token;
        this.scope = scope;
        this.credential = code;
        setAuthenticated(false);
    }

    public EmailCodeAuthenticationToken(String principle, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principle = principle;
        this.token = null;
        this.scope = AuthenticationService.LOGIN;
        this.credential = null;
        super.setAuthenticated(true);
    }

    public VerificationParam toVerificationParam() {
        return new VerificationParam(principle, token, scope, credential);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.credential = null;
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        if (authenticated) {
            throw new IllegalArgumentException("Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        }
        super.setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return credential;
    }

    @Override
    public Object getPrincipal() {
        return principle;
    }
}
