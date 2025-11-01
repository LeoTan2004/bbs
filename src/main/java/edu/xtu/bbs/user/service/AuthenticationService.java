package edu.xtu.bbs.user.service;

import edu.xtu.bbs.user.dto.EmailVerifier;
import edu.xtu.bbs.user.dto.RegisterParam;
import edu.xtu.bbs.user.model.User;

public interface AuthenticationService {
    User signup(RegisterParam registerParam);

    User authenticate(String username, String password);

    User authenticate(EmailVerifier emailVerifier);
}
