package edu.xtu.bbs.user.filter;

import edu.xtu.bbs.user.exception.WeChatAppUserNotFoundException;
import edu.xtu.bbs.user.repo.UserRepository;
import edu.xtu.bbs.user.service.UserBinderService;
import edu.xtu.bbs.user.service.WeChatAppService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class WeChatAppAuthenticationProvider implements AuthenticationProvider {
    private final WeChatAppService weChatAppService;
    private final UserBinderService userBinderService;
    private final UserRepository userRepository;

    public WeChatAppAuthenticationProvider(WeChatAppService weChatAppService, UserBinderService userBinderService, UserRepository userRepository) {
        this.weChatAppService = weChatAppService;
        this.userBinderService = userBinderService;
        this.userRepository = userRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication instanceof WeChatAppAuthenticationToken token) {
            final String code = (String) token.getCredentials();
            try {
                final String openId = weChatAppService.getOpenIdByCode(code);
                final UserDetails userDetails = userBinderService.findUserIdByWeChatOpenId(openId)
                        .flatMap(userRepository::findById)
                        .map(u -> (UserDetails) u)
                        .orElseThrow(() -> new WeChatAppUserNotFoundException(openId, "Cannot find user with WeChat OpenID"));
                return new WeChatAppAuthenticationToken(userDetails, userDetails.getAuthorities());
            } catch (WeChatAppUserNotFoundException e) {
                throw new BadCredentialsException("wechat app user not found", e);
            }
        }
        throw new BadCredentialsException("Unsupported authentication token: " + authentication.getClass());

    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication == WeChatAppAuthenticationToken.class;
    }
}
