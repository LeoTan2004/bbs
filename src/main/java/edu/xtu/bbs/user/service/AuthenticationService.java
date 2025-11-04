package edu.xtu.bbs.user.service;

import edu.xtu.bbs.user.dto.CreateUserRequest;
import edu.xtu.bbs.user.exception.*;
import edu.xtu.bbs.user.model.Role;
import edu.xtu.bbs.user.model.Status;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.repo.UserRepository;
import edu.xtu.bbs.verification.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Authentication related services
 * <p>
 * provide:
 * <li>user registration</li>
 */
@Service
public class AuthenticationService {

    public static final String CHANGE_PWD = "ChangePassword";
    public static final String BIND_EMAIL = "BindEmail";
    public static final String LOGIN = "Login";

    private final UserService userService;
    private final VerificationService verificationService;
    private final AvatarService avatarService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public AuthenticationService(UserService userService, VerificationService verificationService, AvatarService avatarService, PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.userService = userService;
        this.verificationService = verificationService;
        this.avatarService = avatarService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

//    /**
//     * Authenticate user by email verification
//     *
//     * @param param the verification param
//     * @return the authenticated user
//     * @throws EmailNotFoundException               if the email is not found
//     * @throws VerificationRequestNotFoundException if verification request is not found
//     * @throws VerificationTooFrequentException     if verification is too frequent
//     * @throws VerificationScopeIncorrectException  if verification scope is incorrect
//     * @throws VerificationExpiredException         if verification has expired
//     * @throws InvalidVerificationException         if verification is invalid
//     */
//    public User authenticate(VerificationParam param) throws EmailNotFoundException, VerificationRequestNotFoundException, VerificationTooFrequentException, VerificationScopeIncorrectException, VerificationExpiredException, InvalidVerificationException {
//        final User user = userRepository.findByEmail(param.principle()).orElseThrow(
//                () -> new EmailNotFoundException(param.principle(), "Cannot find user by email")
//        );
//        if (!verify(param, LOGIN, param.principle())) {
//            throw new InvalidVerificationException();
//        }
//        return user;
//    }
//
//
//    /**
//     * Pre-login email verification
//     * <p>
//     * Send verify code to email for login request if email exists
//     * </p>
//     *
//     * @param email email address
//     * @return the token that can be used to verify the code
//     * @throws EmailNotFoundException if the email is not found
//     */
//    private String emailLoginVerify(String email) throws EmailNotFoundException {
//        if (!userService.existsByEmail(email)) {
//            throw new EmailNotFoundException(email, "Email not found");
//        }
//        return verificationService.sendCode(email, LOGIN);
//    }

    /**
     * Pre-bind email verification
     * <p>
     * Send verify code to email for binding request if email not exists
     * </p>
     *
     * @param email the email to be bound
     * @return the token that can be used to verify the code
     * @throws EmailAlreadyExistsException if the email is already bound by another user
     */
    public String bindEmailVerify(String email) throws EmailAlreadyExistsException {
        if (userService.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        return verificationService.sendCode(email, BIND_EMAIL);
    }

    public String loginEmailVerify(String email) throws EmailNotFoundException {
        if (!userService.existsByEmail(email)) {
            throw new EmailNotFoundException(email, "Email not found");
        }
        return verificationService.sendCode(email, LOGIN);
    }

    /**
     * Create a new user after verifying the email code
     *
     * @param request           the create user request
     * @param verificationParam the verification param
     * @return the user that has been created
     * @throws UsernameOccupiedException            if the username has been occupied
     * @throws EmailAlreadyExistsException          if the email has been bound by another user
     * @throws InvalidVerificationException         if the verification is invalid
     * @throws VerificationRequestNotFoundException if the verification request is not found
     * @throws VerificationTooFrequentException     if the verification is too frequent
     * @throws VerificationScopeIncorrectException  if the verification scope is incorrect
     * @throws VerificationExpiredException         if the verification has expired
     */
    public User register(CreateUserRequest request, VerificationParam verificationParam)
            throws UsernameOccupiedException, EmailAlreadyExistsException, InvalidVerificationException, VerificationRequestNotFoundException, VerificationTooFrequentException, VerificationScopeIncorrectException, VerificationExpiredException {


        if (userService.existsByUsername(request.username())) {
            throw new UsernameOccupiedException(request.username());
        }

        if (userService.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        if (!verify(verificationParam, BIND_EMAIL, request.email())) {
            throw new InvalidVerificationException();
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setNickname(request.nickname());
        user.setBio(request.bio());
        user.setRole(Role.User);
        final String avatarUrl = avatarService.generateAvatarUrl(user.getUsername());
        user.setAvatarUrl(avatarUrl);
        user.setStatus(Status.Active);
        return userRepository.save(user);
    }


    /**
     * Before changing password, send verification code to user's email
     *
     * @param userId the id of the user which wants to change password
     * @return the token that can be used to verify the code
     * @throws UserNotFoundException if user not found
     */
    public String preUpdatePassword(Integer userId) throws UserNotFoundException {
        final User user = userService.getUserById(userId);
        final String email = user.getEmail();
        return verificationService.sendCode(email, CHANGE_PWD);
    }


    /**
     * Update user's password after verifying the code
     *
     * @param userId      the id of the user which wants to change password
     * @param newPassword the new password
     * @param param       the verification param
     * @return true if update successful, false otherwise
     */
    public boolean updatePassword(Integer userId, String newPassword, VerificationParam param)
            throws UserNotFoundException, VerificationRequestNotFoundException, VerificationTooFrequentException, VerificationExpiredException, VerificationScopeIncorrectException, InvalidVerificationException {

        User user = userService.getUserById(userId);

        if (verify(param, CHANGE_PWD, user.getEmail())) {
            final String encodedNewPassword = passwordEncoder.encode(newPassword);
            if (encodedNewPassword == null || userId == null) {
                return false;
            }
            int updatedRows = userRepository.updatePasswordById(encodedNewPassword, userId);
            return updatedRows > 0;
        }
        return false;
    }

    private boolean verify(VerificationParam param, String scope, String principle) throws VerificationRequestNotFoundException, VerificationTooFrequentException, VerificationExpiredException, InvalidVerificationException, VerificationScopeIncorrectException {

        if (!Objects.equals(principle, param.principle())) {
            throw new InvalidVerificationException();
        }
        if (!Objects.equals(param.scope(), scope)) {
            throw new VerificationScopeIncorrectException();
        }

        return verificationService.verifyCode(param);
    }


}
