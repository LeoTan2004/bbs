package edu.xtu.bbs.user.controller;

import edu.xtu.bbs.user.exception.EmailAlreadyExistsException;
import edu.xtu.bbs.user.exception.EmailNotFoundException;
import edu.xtu.bbs.user.exception.InvalidVerificationException;
import edu.xtu.bbs.user.exception.UsernameOccupiedException;
import edu.xtu.bbs.user.service.AuthenticationService;
import edu.xtu.bbs.user.vo.PasswordUpdateRequest;
import edu.xtu.bbs.user.vo.RegisterVo;
import edu.xtu.bbs.verification.VerificationExpiredException;
import edu.xtu.bbs.verification.VerificationRequestNotFoundException;
import edu.xtu.bbs.verification.VerificationScopeIncorrectException;
import edu.xtu.bbs.verification.VerificationTooFrequentException;
import jakarta.validation.constraints.Email;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register/send-code")
    public String registerByEmail(@Email String email) throws EmailAlreadyExistsException {
        return authenticationService.bindEmailVerify(email);
    }

    @PostMapping("/register")
    public String register(@RequestBody RegisterVo registerVo) throws VerificationRequestNotFoundException, VerificationTooFrequentException, UsernameOccupiedException, VerificationScopeIncorrectException, EmailAlreadyExistsException, VerificationExpiredException, InvalidVerificationException {
        return authenticationService.register(registerVo.getUser(), registerVo.getVerification()).getEmail();
    }

    @PostMapping("/login/send-code")
    public String loginByEmail(@Email String email) throws EmailNotFoundException {
        return authenticationService.loginEmailVerify(email);
    }

    @PostMapping("/reset-password/send-code")
    public String resetPasswordByEmail(@Email String email) throws EmailNotFoundException {
        return authenticationService.preUpdatePassword(email);
    }

    @PostMapping("/reset-password")
    public Boolean resetPassword(@RequestBody PasswordUpdateRequest request) throws VerificationRequestNotFoundException, VerificationTooFrequentException, EmailNotFoundException, VerificationScopeIncorrectException, VerificationExpiredException, InvalidVerificationException {
        return authenticationService.updatePassword(request.getEmail(), request.getPassword(), request.getVerification());
    }


}
