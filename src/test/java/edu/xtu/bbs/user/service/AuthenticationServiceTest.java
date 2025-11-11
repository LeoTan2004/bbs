package edu.xtu.bbs.user.service;

import edu.xtu.bbs.user.dto.CreateUserRequest;
import edu.xtu.bbs.user.exception.EmailAlreadyExistsException;
import edu.xtu.bbs.user.exception.EmailNotFoundException;
import edu.xtu.bbs.user.exception.InvalidVerificationException;
import edu.xtu.bbs.user.exception.UsernameOccupiedException;
import edu.xtu.bbs.user.model.Role;
import edu.xtu.bbs.user.model.Status;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.repo.UserRepository;
import edu.xtu.bbs.verification.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Tests")
class AuthenticationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private VerificationService verificationService;

    @Mock
    private AvatarService avatarService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBinderService userBinderService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private CreateUserRequest createUserRequest;
    private VerificationParam verificationParam;

    @BeforeEach
    void setUp() {
        User testUser = createTestUser();
        createUserRequest = new CreateUserRequest(
                "testuser",
                "password123",
                "test@example.com",
                "Test User",
                "Test bio"
        );
        verificationParam = new VerificationParam(
                "test@example.com",
                "verification-token",
                "BindEmail",
                "123456"
        );
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setPasswordHash("$2a$10$encodedPassword");
        user.setNickname("Test User");
        user.setBio("Test bio");
        user.setRole(Role.User);
        user.setStatus(Status.Active);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }

    @Test
    @DisplayName("Should bind email verification successfully when email does not exist")
    void bindEmailVerify_WhenEmailDoesNotExist_ShouldReturnToken() throws EmailAlreadyExistsException {
        // Given
        String email = "new@example.com";
        String expectedToken = "verification-token";

        when(userService.existsByEmail(email)).thenReturn(false);
        when(verificationService.sendCode(email, AuthenticationService.BIND_EMAIL))
                .thenReturn(expectedToken);

        // When
        String result = authenticationService.bindEmailVerify(email);

        // Then
        assertThat(result).isEqualTo(expectedToken);
        verify(userService).existsByEmail(email);
        verify(verificationService).sendCode(email, AuthenticationService.BIND_EMAIL);
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when email already exists")
    void bindEmailVerify_WhenEmailExists_ShouldThrowEmailAlreadyExistsException() {
        // Given
        String email = "existing@example.com";
        when(userService.existsByEmail(email)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authenticationService.bindEmailVerify(email))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userService).existsByEmail(email);
        verifyNoInteractions(verificationService);
    }

    @Test
    @DisplayName("Should login email verification successfully when email exists")
    void loginEmailVerify_WhenEmailExists_ShouldReturnToken() throws EmailNotFoundException {
        // Given
        String email = "test@example.com";
        String expectedToken = "verification-token";

        when(userService.existsByEmail(email)).thenReturn(true);
        when(verificationService.sendCode(email, AuthenticationService.LOGIN))
                .thenReturn(expectedToken);

        // When
        String result = authenticationService.loginEmailVerify(email);

        // Then
        assertThat(result).isEqualTo(expectedToken);
        verify(userService).existsByEmail(email);
        verify(verificationService).sendCode(email, AuthenticationService.LOGIN);
    }

    @Test
    @DisplayName("Should throw EmailNotFoundException when email does not exist for login")
    void loginEmailVerify_WhenEmailDoesNotExist_ShouldThrowEmailNotFoundException() {
        // Given
        String email = "nonexistent@example.com";
        when(userService.existsByEmail(email)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authenticationService.loginEmailVerify(email))
                .isInstanceOf(EmailNotFoundException.class)
                .hasMessage("Email not found");

        verify(userService).existsByEmail(email);
        verifyNoInteractions(verificationService);
    }

    @Test
    @DisplayName("Should register user successfully with valid data and verification")
    void register_WithValidDataAndVerification_ShouldCreateUser() throws Exception {
        // Given
        String encodedPassword = "$2a$10$encodedPassword";
        String avatarUrl = "https://example.com/avatar.jpg";
        User savedUser = createTestUser();

        when(userService.existsByUsername(createUserRequest.username())).thenReturn(false);
        when(userService.existsByEmail(createUserRequest.email())).thenReturn(false);
        when(verificationService.verifyCode(verificationParam)).thenReturn(true);
        when(passwordEncoder.encode(createUserRequest.password())).thenReturn(encodedPassword);
        when(avatarService.generateAvatarUrl(createUserRequest.username())).thenReturn(avatarUrl);
        when(userRepository.save(any())).thenReturn(savedUser);

        // When
        User result = authenticationService.register(createUserRequest, verificationParam);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");

        // Verify user creation
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertThat(capturedUser.getUsername()).isEqualTo(createUserRequest.username());
        assertThat(capturedUser.getPasswordHash()).isEqualTo(encodedPassword);
        assertThat(capturedUser.getNickname()).isEqualTo(createUserRequest.nickname());
        assertThat(capturedUser.getBio()).isEqualTo(createUserRequest.bio());
        assertThat(capturedUser.getRole()).isEqualTo(Role.User);
        assertThat(capturedUser.getAvatarUrl()).isEqualTo(avatarUrl);
        assertThat(capturedUser.getStatus()).isEqualTo(Status.Active);

        verify(userService).existsByUsername(createUserRequest.username());
        verify(userService).existsByEmail(createUserRequest.email());
        verify(verificationService).verifyCode(verificationParam);
        verify(passwordEncoder).encode(createUserRequest.password());
        verify(avatarService).generateAvatarUrl(createUserRequest.username());
        verify(userBinderService).bindEmail(result, createUserRequest.email());
    }

    @Test
    @DisplayName("Should throw UsernameOccupiedException when username already exists")
    void register_WhenUsernameExists_ShouldThrowUsernameOccupiedException() {
        // Given
        when(userService.existsByUsername(createUserRequest.username())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authenticationService.register(createUserRequest, verificationParam))
                .isInstanceOf(UsernameOccupiedException.class);

        verify(userService).existsByUsername(createUserRequest.username());
        verify(userService, never()).existsByEmail(anyString());
        verifyNoInteractions(verificationService);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when email already exists")
    void register_WhenEmailExists_ShouldThrowEmailAlreadyExistsException() {
        // Given
        when(userService.existsByUsername(createUserRequest.username())).thenReturn(false);
        when(userService.existsByEmail(createUserRequest.email())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authenticationService.register(createUserRequest, verificationParam))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userService).existsByUsername(createUserRequest.username());
        verify(userService).existsByEmail(createUserRequest.email());
        verifyNoInteractions(verificationService);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should throw InvalidVerificationException when verification fails")
    void register_WhenVerificationFails_ShouldThrowInvalidVerificationException() throws Exception {
        // Given
        when(userService.existsByUsername(createUserRequest.username())).thenReturn(false);
        when(userService.existsByEmail(createUserRequest.email())).thenReturn(false);
        when(verificationService.verifyCode(verificationParam)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authenticationService.register(createUserRequest, verificationParam))
                .isInstanceOf(InvalidVerificationException.class);

        verify(userService).existsByUsername(createUserRequest.username());
        verify(userService).existsByEmail(createUserRequest.email());
        verify(verificationService).verifyCode(verificationParam);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should throw InvalidVerificationException when principle doesn't match email")
    void register_WhenPrincipleDoesNotMatchEmail_ShouldThrowInvalidVerificationException() {
        // Given
        VerificationParam wrongPrincipleParam = new VerificationParam(
                "wrong@example.com",
                "verification-token",
                "BindEmail",
                "123456"
        );

        when(userService.existsByUsername(createUserRequest.username())).thenReturn(false);
        when(userService.existsByEmail(createUserRequest.email())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authenticationService.register(createUserRequest, wrongPrincipleParam))
                .isInstanceOf(InvalidVerificationException.class);

        verify(userService).existsByUsername(createUserRequest.username());
        verify(userService).existsByEmail(createUserRequest.email());
        verifyNoInteractions(verificationService);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should throw VerificationScopeIncorrectException when scope is incorrect")
    void register_WhenScopeIsIncorrect_ShouldThrowVerificationScopeIncorrectException() {
        // Given
        VerificationParam wrongScopeParam = new VerificationParam(
                "test@example.com",
                "verification-token",
                "Login",
                "123456"
        );

        when(userService.existsByUsername(createUserRequest.username())).thenReturn(false);
        when(userService.existsByEmail(createUserRequest.email())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authenticationService.register(createUserRequest, wrongScopeParam))
                .isInstanceOf(VerificationScopeIncorrectException.class);

        verify(userService).existsByUsername(createUserRequest.username());
        verify(userService).existsByEmail(createUserRequest.email());
        verifyNoInteractions(verificationService);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should send verification code for password update successfully")
    void preUpdatePassword_WhenEmailExists_ShouldReturnToken() throws EmailNotFoundException {
        // Given
        String email = "test@example.com";
        String expectedToken = "verification-token";

        when(userService.existsByEmail(email)).thenReturn(true);
        when(verificationService.sendCode(email, AuthenticationService.CHANGE_PWD))
                .thenReturn(expectedToken);

        // When
        String result = authenticationService.preUpdatePassword(email);

        // Then
        assertThat(result).isEqualTo(expectedToken);
        verify(userService).existsByEmail(email);
        verify(verificationService).sendCode(email, AuthenticationService.CHANGE_PWD);
    }

    @Test
    @DisplayName("Should throw EmailNotFoundException when email does not exist for password update")
    void preUpdatePassword_WhenEmailDoesNotExist_ShouldThrowEmailNotFoundException() {
        // Given
        String email = "nonexistent@example.com";
        when(userService.existsByEmail(email)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authenticationService.preUpdatePassword(email))
                .isInstanceOf(EmailNotFoundException.class);

        verify(userService).existsByEmail(email);
        verifyNoInteractions(verificationService);
    }

    @Test
    @DisplayName("Should update password successfully with valid verification")
    void updatePassword_WithValidVerification_ShouldReturnTrue() throws Exception {
        // Given
        String email = "test@example.com";
        String newPassword = "newpassword123";
        String encodedPassword = "$2a$10$newEncodedPassword";
        Integer userId = 1;

        VerificationParam passwordParam = new VerificationParam(
                email,
                "verification-token",
                AuthenticationService.CHANGE_PWD,
                "123456"
        );

        when(userService.existsByEmail(email)).thenReturn(true);
        when(verificationService.verifyCode(passwordParam)).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(userBinderService.findUserIdByEmail(email)).thenReturn(java.util.Optional.of(userId));
        when(userRepository.updatePasswordById(encodedPassword, userId)).thenReturn(1);

        // When
        boolean result = authenticationService.updatePassword(email, newPassword, passwordParam);

        // Then
        assertThat(result).isTrue();
        verify(userService).existsByEmail(email);
        verify(verificationService).verifyCode(passwordParam);
        verify(passwordEncoder).encode(newPassword);
        verify(userBinderService).findUserIdByEmail(email);
        verify(userRepository).updatePasswordById(encodedPassword, userId);
    }

    @Test
    @DisplayName("Should return false when password update fails in database")
    void updatePassword_WhenDatabaseUpdateFails_ShouldReturnFalse() throws Exception {
        // Given
        String email = "test@example.com";
        String newPassword = "newpassword123";
        String encodedPassword = "$2a$10$newEncodedPassword";
        Integer userId = 1;

        VerificationParam passwordParam = new VerificationParam(
                email,
                "verification-token",
                AuthenticationService.CHANGE_PWD,
                "123456"
        );

        when(userService.existsByEmail(email)).thenReturn(true);
        when(verificationService.verifyCode(passwordParam)).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(userBinderService.findUserIdByEmail(email)).thenReturn(java.util.Optional.of(userId));
        when(userRepository.updatePasswordById(encodedPassword, userId)).thenReturn(0);

        // When
        boolean result = authenticationService.updatePassword(email, newPassword, passwordParam);

        // Then
        assertThat(result).isFalse();
        verify(userService).existsByEmail(email);
        verify(verificationService).verifyCode(passwordParam);
        verify(passwordEncoder).encode(newPassword);
        verify(userBinderService).findUserIdByEmail(email);
        verify(userRepository).updatePasswordById(encodedPassword, userId);
    }

    @Test
    @DisplayName("Should return false when verification fails for password update")
    void updatePassword_WhenVerificationFails_ShouldReturnFalse() throws Exception {
        // Given
        String email = "test@example.com";
        String newPassword = "newpassword123";

        VerificationParam passwordParam = new VerificationParam(
                email,
                "verification-token",
                AuthenticationService.CHANGE_PWD,
                "123456"
        );

        when(userService.existsByEmail(email)).thenReturn(true);
        when(verificationService.verifyCode(passwordParam)).thenReturn(false);

        // When
        boolean result = authenticationService.updatePassword(email, newPassword, passwordParam);

        // Then
        assertThat(result).isFalse();
        verify(userService).existsByEmail(email);
        verify(verificationService).verifyCode(passwordParam);
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).updatePasswordById(anyString(), anyInt());
    }

    @Test
    @DisplayName("Should throw EmailNotFoundException when email does not exist for password update")
    void updatePassword_WhenEmailDoesNotExist_ShouldThrowEmailNotFoundException() {
        // Given
        String email = "nonexistent@example.com";
        String newPassword = "newpassword123";

        VerificationParam passwordParam = new VerificationParam(
                email,
                "verification-token",
                AuthenticationService.CHANGE_PWD,
                "123456"
        );

        when(userService.existsByEmail(email)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authenticationService.updatePassword(email, newPassword, passwordParam))
                .isInstanceOf(EmailNotFoundException.class);

        verify(userService).existsByEmail(email);
        verifyNoInteractions(verificationService);
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).updatePasswordById(anyString(), anyInt());
    }

    @Test
    @DisplayName("Should throw InvalidVerificationException when principle doesn't match email")
    void updatePassword_WhenPrincipleDoesNotMatchEmail_ShouldThrowInvalidVerificationException() {
        // Given
        String email = "test@example.com";
        String newPassword = "newpassword123";

        VerificationParam passwordParam = new VerificationParam(
                "wrong@example.com",
                "verification-token",
                AuthenticationService.CHANGE_PWD,
                "123456"
        );

        when(userService.existsByEmail(email)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authenticationService.updatePassword(email, newPassword, passwordParam))
                .isInstanceOf(InvalidVerificationException.class);

        verify(userService).existsByEmail(email);
        verifyNoInteractions(verificationService);
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).updatePasswordById(anyString(), anyInt());
    }

    @Test
    @DisplayName("Should throw VerificationScopeIncorrectException when scope is incorrect for password update")
    void updatePassword_WhenScopeIsIncorrect_ShouldThrowVerificationScopeIncorrectException() {
        // Given
        String email = "test@example.com";
        String newPassword = "newpassword123";

        VerificationParam passwordParam = new VerificationParam(
                email,
                "verification-token",
                "Login",
                "123456"
        );

        when(userService.existsByEmail(email)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authenticationService.updatePassword(email, newPassword, passwordParam))
                .isInstanceOf(VerificationScopeIncorrectException.class);

        verify(userService).existsByEmail(email);
        verifyNoInteractions(verificationService);
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).updatePasswordById(anyString(), anyInt());
    }

    @Test
    @DisplayName("Should return false when encoded password is null")
    void updatePassword_WhenEncodedPasswordIsNull_ShouldReturnFalse() throws Exception {
        // Given
        String email = "test@example.com";
        String newPassword = "newpassword123";
        Integer userId = 1;

        VerificationParam passwordParam = new VerificationParam(
                email,
                "verification-token",
                AuthenticationService.CHANGE_PWD,
                "123456"
        );

        when(userService.existsByEmail(email)).thenReturn(true);
        when(verificationService.verifyCode(passwordParam)).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(null);
        when(userBinderService.findUserIdByEmail(email)).thenReturn(java.util.Optional.of(userId));

        // When
        boolean result = authenticationService.updatePassword(email, newPassword, passwordParam);

        // Then
        assertThat(result).isFalse();
        verify(userService).existsByEmail(email);
        verify(verificationService).verifyCode(passwordParam);
        verify(passwordEncoder).encode(newPassword);
        verify(userBinderService).findUserIdByEmail(email);
        verify(userRepository, never()).updatePasswordById(anyString(), anyInt());
    }

    @Test
    @DisplayName("Should propagate VerificationRequestNotFoundException")
    void updatePassword_WhenVerificationRequestNotFound_ShouldThrowVerificationRequestNotFoundException() throws Exception {
        // Given
        String email = "test@example.com";
        String newPassword = "newpassword123";

        VerificationParam passwordParam = new VerificationParam(
                email,
                "verification-token",
                AuthenticationService.CHANGE_PWD,
                "123456"
        );

        when(userService.existsByEmail(email)).thenReturn(true);
        when(verificationService.verifyCode(passwordParam))
                .thenThrow(new VerificationRequestNotFoundException());

        // When & Then
        assertThatThrownBy(() -> authenticationService.updatePassword(email, newPassword, passwordParam))
                .isInstanceOf(VerificationRequestNotFoundException.class);

        verify(userService).existsByEmail(email);
        verify(verificationService).verifyCode(passwordParam);
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).updatePasswordById(anyString(), anyInt());
    }

    @Test
    @DisplayName("Should propagate VerificationExpiredException")
    void updatePassword_WhenVerificationExpired_ShouldThrowVerificationExpiredException() throws Exception {
        // Given
        String email = "test@example.com";
        String newPassword = "newpassword123";

        VerificationParam passwordParam = new VerificationParam(
                email,
                "verification-token",
                AuthenticationService.CHANGE_PWD,
                "123456"
        );

        when(userService.existsByEmail(email)).thenReturn(true);
        when(verificationService.verifyCode(passwordParam))
                .thenThrow(new VerificationExpiredException());

        // When & Then
        assertThatThrownBy(() -> authenticationService.updatePassword(email, newPassword, passwordParam))
                .isInstanceOf(VerificationExpiredException.class);

        verify(userService).existsByEmail(email);
        verify(verificationService).verifyCode(passwordParam);
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).updatePasswordById(anyString(), anyInt());
    }

}