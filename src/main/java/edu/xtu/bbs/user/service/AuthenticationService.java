package edu.xtu.bbs.user.service;

import edu.xtu.bbs.user.dto.CreateUserRequest;
import edu.xtu.bbs.user.dto.WeChatRegisterRequest;
import edu.xtu.bbs.user.exception.*;
import edu.xtu.bbs.user.model.Role;
import edu.xtu.bbs.user.model.Status;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.repo.UserRepository;
import edu.xtu.bbs.verification.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Authentication related services
 * <p>
 * provide:
 * <li>user registration</li>
 */
@Slf4j
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
    private final UserBinderService userBinderService;
    private final WeChatAppService weChatAppService;

    public AuthenticationService(UserService userService, VerificationService verificationService,
                                 AvatarService avatarService, PasswordEncoder passwordEncoder, UserRepository userRepository,
                                 UserBinderService userBinderService, WeChatAppService weChatAppService) {
        this.userService = userService;
        this.verificationService = verificationService;
        this.avatarService = avatarService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.userBinderService = userBinderService;
        this.weChatAppService = weChatAppService;
    }

    /**
     * Pre-bind email verification
     * <p>
     * Send verify code to email for binding request if email not exists
     * </p>
     *
     * @param email the email to be bound
     * @return the token that can be used to verify the code
     * @throws EmailAlreadyExistsException if the email is already bound by another
     *                                     user
     */
    public String bindEmailVerify(String email) throws EmailAlreadyExistsException {
        if (userService.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        return verificationService.sendCode(email, BIND_EMAIL);
    }

    /**
     * Pre-login email verification
     * <p>
     * Send verify code to email for login request if email exists
     * </p>
     *
     * @param email the email to be used for login
     * @return the token that can be used to verify the code
     * @throws EmailNotFoundException if the email is not found
     */
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
     * @throws UsernameOccupiedException            if the username has been
     *                                              occupied
     * @throws EmailAlreadyExistsException          if the email has been bound by
     *                                              another user
     * @throws InvalidVerificationException         if the verification is invalid
     * @throws VerificationRequestNotFoundException if the verification request is
     *                                              not found
     * @throws VerificationTooFrequentException     if the verification is too
     *                                              frequent
     * @throws VerificationScopeIncorrectException  if the verification scope is
     *                                              incorrect
     * @throws VerificationExpiredException         if the verification has expired
     */
    public User register(CreateUserRequest request, VerificationParam verificationParam)
            throws UsernameOccupiedException, EmailAlreadyExistsException, InvalidVerificationException,
            VerificationRequestNotFoundException, VerificationTooFrequentException, VerificationScopeIncorrectException,
            VerificationExpiredException {

        if (request == null) {
            throw new IllegalArgumentException("Invalid registration request");
        }

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
        user.setNickname(request.nickname());
        user.setBio(request.bio());
        user.setRole(Role.User);
        final String avatarUrl = avatarService.generateAvatarUrl(user.getUsername());
        user.setAvatarUrl(avatarUrl);
        user.setStatus(Status.Active);

        User savedUser = userRepository.save(user);

        // Bind email to user
        userBinderService.bindEmail(savedUser, request.email());

        return savedUser;
    }

    /**
     * Register user with WeChat
     * <p>
     * Register a new user using WeChat authorization code
     * </p>
     * 
     * @param request WeChat registration request containing code and user info
     * @return the user that has been created
     * @throws UsernameOccupiedException          if the username has been occupied
     * @throws WeChatOpenIdAlreadyExistsException if the WeChat OpenID has been
     *                                            bound by another user
     * @throws InvalidWeChatCodeException         if the WeChat code is invalid
     */
    @Transactional
    public User registerWithWeChat(WeChatRegisterRequest request)
            throws UsernameOccupiedException, WeChatOpenIdAlreadyExistsException, InvalidWeChatCodeException {

        if (request == null) {
            throw new IllegalArgumentException("Invalid WeChat registration request");
        }

        // Check if username already exists
        if (userService.existsByUsername(request.username())) {
            throw new UsernameOccupiedException(request.username());
        }
        String openId;
        // Get OpenID from WeChat using the authorization code
        try {
            openId = weChatAppService.getOpenIdByCode(request.code());
            if (openId == null || openId.trim().isEmpty()) {
                throw new IllegalArgumentException("Failed to get WeChat OpenID from code");
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidWeChatCodeException("Failed to get WeChat OpenID: " + e.getMessage(), e);
        }

        // Check if WeChat OpenID already exists
        if (userBinderService.existsByWeChatOpenId(openId)) {
            throw new WeChatOpenIdAlreadyExistsException(openId);
        }

        // Create new user
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname() != null ? request.nickname() : request.username());
        user.setBio(request.bio());
        user.setRole(Role.User);

        // Generate avatar URL
        final String avatarUrl = avatarService.generateAvatarUrl(user.getUsername());
        user.setAvatarUrl(avatarUrl);
        user.setStatus(Status.Active);

        // Save user
        User savedUser = userRepository.save(user);

        // Bind WeChat OpenID to user
        userBinderService.bindWeChat(savedUser, openId);

        return savedUser;
    }

    public String preUpdatePassword(String email) throws EmailNotFoundException {
        if (!userService.existsByEmail(email)) {
            throw new EmailNotFoundException(email, "Email not found: " + email);
        }

        return verificationService.sendCode(email, CHANGE_PWD);
    }

    public boolean updatePassword(String email, String newPassword, VerificationParam param)
            throws VerificationRequestNotFoundException, VerificationTooFrequentException, VerificationExpiredException,
            VerificationScopeIncorrectException, InvalidVerificationException, EmailNotFoundException {

        if (!userService.existsByEmail(email)) {
            throw new EmailNotFoundException(email, "Email not found");
        }

        if (verify(param, CHANGE_PWD, email)) {
            final String encodedNewPassword = passwordEncoder.encode(newPassword);
            final Integer userId = userBinderService.findUserIdByEmail(email).orElse(null);

            if (encodedNewPassword == null || userId == null) {
                return false;
            }

            int updatedRows = userRepository.updatePasswordById(encodedNewPassword, userId);
            return updatedRows > 0;
        }

        return false;
    }

    private boolean verify(VerificationParam param, String scope, String principle)
            throws VerificationRequestNotFoundException, VerificationTooFrequentException, VerificationExpiredException,
            InvalidVerificationException, VerificationScopeIncorrectException {

        if (!Objects.equals(principle, param.principle())) {
            throw new InvalidVerificationException();
        }
        if (!Objects.equals(param.scope(), scope)) {
            throw new VerificationScopeIncorrectException();
        }

        return verificationService.verifyCode(param);
    }

    public User getCurrentUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null; // or throw an exception if you prefer
        }

        if (authentication.getPrincipal() instanceof User user) {
            return user;
        } else if (authentication.getPrincipal() instanceof String username) {
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("couldn't find user: " + username));
        } else {
            throw new IllegalStateException(
                    "Unexpected principal type: " + authentication.getPrincipal().getClass().getName());
        }
    }
}
