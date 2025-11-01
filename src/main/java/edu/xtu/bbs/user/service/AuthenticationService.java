package edu.xtu.bbs.user.service;

import edu.xtu.bbs.user.dto.RegisterParam;
import edu.xtu.bbs.user.exception.InvalidUsernameException;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.verification.VerificationExpiredException;
import edu.xtu.bbs.verification.VerificationParam;

public interface AuthenticationService {
    User signup(RegisterParam registerParam) throws InvalidUsernameException;

    User authenticate(String username, String password);

    User authenticate(VerificationParam verificationParam) throws VerificationExpiredException;
}
