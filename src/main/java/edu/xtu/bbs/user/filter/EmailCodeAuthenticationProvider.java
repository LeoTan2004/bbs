package edu.xtu.bbs.user.filter;

import edu.xtu.bbs.user.exception.EmailNotFoundException;
import edu.xtu.bbs.user.repo.UserRepository;
import edu.xtu.bbs.verification.*;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class EmailCodeAuthenticationProvider implements AuthenticationProvider {


    private final VerificationService verificationService;
    private final UserRepository userRepository;

    public EmailCodeAuthenticationProvider(VerificationService verificationService, UserRepository userRepository) {
        this.verificationService = verificationService;
        this.userRepository = userRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication instanceof EmailCodeAuthenticationToken token) {
            final VerificationParam verificationParam = token.toVerificationParam();
            try {
                final boolean b = verificationService.verifyCode(verificationParam);
                if (!b) {
                    throw new BadCredentialsException("code was not correct");
                }
                final UserDetails user = userRepository.findByEmail(verificationParam.principle()).orElseThrow(
                        () -> new UsernameNotFoundException("Could not find user", new EmailNotFoundException(verificationParam.principle(), "Cannot find user with email"))
                );
                final EmailCodeAuthenticationToken authenticationToken = new EmailCodeAuthenticationToken(user.getUsername(), user.getAuthorities());
                authenticationToken.setDetails(user);
                return authenticationToken;
            } catch (VerificationRequestNotFoundException | VerificationExpiredException |
                     VerificationTooFrequentException e) {
                throw new BadCredentialsException("email verification failed", e);
            }
        }
        throw new BadCredentialsException("Unsupported authentication token: " + authentication.getClass());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication == EmailCodeAuthenticationToken.class;
    }
}
