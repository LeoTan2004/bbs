package edu.xtu.bbs.user.controller;

import edu.xtu.bbs.user.dto.WeChatRegisterRequest;
import edu.xtu.bbs.user.exception.EmailAlreadyExistsException;
import edu.xtu.bbs.user.exception.EmailNotFoundException;
import edu.xtu.bbs.user.exception.InvalidVerificationException;
import edu.xtu.bbs.user.exception.UsernameOccupiedException;
import edu.xtu.bbs.user.exception.WeChatOpenIdAlreadyExistsException;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.service.AuthenticationService;
import edu.xtu.bbs.user.service.UserBinderService;
import edu.xtu.bbs.user.vo.*;
import edu.xtu.bbs.verification.VerificationExpiredException;
import edu.xtu.bbs.verification.VerificationRequestNotFoundException;
import edu.xtu.bbs.verification.VerificationScopeIncorrectException;
import edu.xtu.bbs.verification.VerificationTooFrequentException;
import jakarta.validation.constraints.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserBinderService userBinderService;

    public AuthController(AuthenticationService authenticationService, UserBinderService userBinderService) {
        this.authenticationService = authenticationService;
        this.userBinderService = userBinderService;
    }

    @PostMapping("/register/send-code")
    public AuthTokenResponse registerByEmail(@Email String email) throws EmailAlreadyExistsException {
        String token = authenticationService.bindEmailVerify(email);
        return new AuthTokenResponse(token);
    }

    @PostMapping("/register")
    public RegisterResponse register(@RequestBody RegisterVo registerVo) throws VerificationRequestNotFoundException, VerificationTooFrequentException, UsernameOccupiedException, VerificationScopeIncorrectException, EmailAlreadyExistsException, VerificationExpiredException, InvalidVerificationException {
        if (registerVo == null || registerVo.getUser() == null) {
            throw new IllegalArgumentException("Invalid registration request");
        }

        User registeredUser = authenticationService.register(registerVo.getUser(), registerVo.getVerification());
        String email = userBinderService.findEmailByUserId(registeredUser.getId())
                .orElse(registerVo.getUser().email()); // Fallback to email from request
        return new RegisterResponse(email);
    }

    @PostMapping("/register/wechat")
    public WeChatRegisterResponse registerWithWeChat(@RequestBody WeChatRegisterRequest request) 
            throws UsernameOccupiedException, WeChatOpenIdAlreadyExistsException {
        if (request == null) {
            throw new IllegalArgumentException("Invalid WeChat registration request");
        }

        User registeredUser = authenticationService.registerWithWeChat(request);
        String openId = userBinderService.findWeChatOpenIdByUserId(registeredUser.getId())
                .orElse(""); // Fallback to empty string if not found
        return new WeChatRegisterResponse(openId);
    }

    @PostMapping("/login/send-code")
    public AuthTokenResponse loginByEmail(@Email String email) throws EmailNotFoundException {
        String token = authenticationService.loginEmailVerify(email);
        return new AuthTokenResponse(token);
    }

    @PostMapping("/reset-password/send-code")
    public AuthTokenResponse resetPasswordByEmail(@Email String email) throws EmailNotFoundException {
        String token = authenticationService.preUpdatePassword(email);
        return new AuthTokenResponse(token);
    }

    @PostMapping("/reset-password")
    public PasswordResetResponse resetPassword(@RequestBody PasswordUpdateRequest request) throws VerificationRequestNotFoundException, VerificationTooFrequentException, EmailNotFoundException, VerificationScopeIncorrectException, VerificationExpiredException, InvalidVerificationException {
        if (request == null) {
            throw new IllegalArgumentException("Invalid password reset request");
        }
        
        boolean success = authenticationService.updatePassword(request.getEmail(), request.getPassword(), request.getVerification());
        return new PasswordResetResponse(success);
    }


}
