package edu.xtu.bbs.user.controller;

import edu.xtu.bbs.user.exception.EmailAlreadyExistsException;
import edu.xtu.bbs.user.exception.EmailNotFoundException;
import edu.xtu.bbs.user.exception.InvalidVerificationException;
import edu.xtu.bbs.user.exception.UsernameOccupiedException;
import edu.xtu.bbs.user.service.AuthenticationService;
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

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register/send-code")
    public AuthTokenResponse registerByEmail(@Email String email) throws EmailAlreadyExistsException {
        String token = authenticationService.bindEmailVerify(email);
        return new AuthTokenResponse(token);
    }

    @PostMapping("/register")
    public RegisterResponse register(@RequestBody RegisterVo registerVo) throws VerificationRequestNotFoundException, VerificationTooFrequentException, UsernameOccupiedException, VerificationScopeIncorrectException, EmailAlreadyExistsException, VerificationExpiredException, InvalidVerificationException {
        String email = authenticationService.register(registerVo.getUser(), registerVo.getVerification()).getEmail();
        return new RegisterResponse(email);
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
        boolean success = authenticationService.updatePassword(request.getEmail(), request.getPassword(), request.getVerification());
        return new PasswordResetResponse(success);
    }


}
