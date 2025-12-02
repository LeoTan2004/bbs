package edu.xtu.bbs.user.service;

import edu.xtu.bbs.common.validation.ContentAuditService;
import edu.xtu.bbs.user.dto.UpdateProfileRequest;
import edu.xtu.bbs.user.exception.UserNotFoundException;
import edu.xtu.bbs.user.model.Role;
import edu.xtu.bbs.user.model.Status;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBinderService userBinderService;

    @Mock
    private ContentAuditService contentAuditService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1);
        user.setUsername("testuser");
        user.setPasswordHash("$2a$10$encodedPassword");
        user.setNickname("Test User");
        user.setBio("Test bio");
        user.setProfileSlug("test-profile");
        user.setAvatarUrl("https://example.com/avatar.jpg");
        user.setRole(Role.User);
        user.setStatus(Status.Active);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void getUserById_WhenUserExists_ShouldReturnUser() throws UserNotFoundException {
        // Given
        Integer userId = 1;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserById(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user ID is null")
    void getUserById_WhenIdIsNull_ShouldThrowUserNotFoundException() {
        // When & Then
        assertThatThrownBy(() -> userService.getUserById(null))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).findById(anyInt());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user does not exist")
    void getUserById_WhenUserDoesNotExist_ShouldThrowUserNotFoundException() {
        // Given
        Integer userId = 999;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should return true when email exists")
    void existsByEmail_WhenEmailExists_ShouldReturnTrue() {
        // Given
        String email = "test@example.com";
        when(userBinderService.existsByEmail(email)).thenReturn(true);

        // When
        boolean result = userService.existsByEmail(email);

        // Then
        assertThat(result).isTrue();
        verify(userBinderService).existsByEmail(email);
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void existsByEmail_WhenEmailDoesNotExist_ShouldReturnFalse() {
        // Given
        String email = "nonexistent@example.com";
        when(userBinderService.existsByEmail(email)).thenReturn(false);

        // When
        boolean result = userService.existsByEmail(email);

        // Then
        assertThat(result).isFalse();
        verify(userBinderService).existsByEmail(email);
    }

    @Test
    @DisplayName("Should return true when username exists")
    void existsByUsername_WhenUsernameExists_ShouldReturnTrue() {
        // Given
        String username = "testuser";
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // When
        boolean result = userService.existsByUsername(username);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsByUsername(username);
    }

    @Test
    @DisplayName("Should return false when username does not exist")
    void existsByUsername_WhenUsernameDoesNotExist_ShouldReturnFalse() {
        // Given
        String username = "nonexistentuser";
        when(userRepository.existsByUsername(username)).thenReturn(false);

        // When
        boolean result = userService.existsByUsername(username);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).existsByUsername(username);
    }

    @Test
    @DisplayName("Should find user by username successfully")
    void findByUsername_WhenUserExists_ShouldReturnUserDetails() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // When
        UserDetails result = userService.findByUsername(username);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getPassword()).isEqualTo("$2a$10$encodedPassword");
        assertThat(result.isEnabled()).isTrue();
        verify(userRepository).findByUsername(username);
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user does not exist")
    void findByUsername_WhenUserDoesNotExist_ShouldThrowUsernameNotFoundException() {
        // Given
        String username = "nonexistentuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.findByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage(username);

        verify(userRepository).findByUsername(username);
    }

    @Test
    @DisplayName("Should update profile successfully")
    void updateProfile_WhenUserExistsAndUpdateSuccessful_ShouldReturnTrue() throws UserNotFoundException {
        // Given
        Integer userId = 1;
        UpdateProfileRequest request = new UpdateProfileRequest(
                "New Nickname",
                "New bio",
                "new-profile-slug"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.updateProfileById(
                request.nickname(),
                request.bio(),
                request.profileSlug(),
                userId
        )).thenReturn(1);

        // When
        boolean result = userService.updateProfile(userId, request);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).findById(userId);
        verify(userRepository).updateProfileById(
                "New Nickname",
                "New bio",
                "new-profile-slug",
                userId
        );
        verify(contentAuditService).validate(request);
    }

    @Test
    @DisplayName("Should return false when profile update fails")
    void updateProfile_WhenUpdateFails_ShouldReturnFalse() throws UserNotFoundException {
        // Given
        Integer userId = 1;
        UpdateProfileRequest request = new UpdateProfileRequest(
                "New Nickname",
                "New bio",
                "new-profile-slug"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.updateProfileById(
                request.nickname(),
                request.bio(),
                request.profileSlug(),
                userId
        )).thenReturn(0);

        // When
        boolean result = userService.updateProfile(userId, request);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).findById(userId);
        verify(userRepository).updateProfileById(
                "New Nickname",
                "New bio",
                "new-profile-slug",
                userId
        );
        verify(contentAuditService).validate(request);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when updating profile of non-existent user")
    void updateProfile_WhenUserDoesNotExist_ShouldThrowUserNotFoundException() {
        // Given
        Integer userId = 999;
        UpdateProfileRequest request = new UpdateProfileRequest(
                "New Nickname",
                "New bio",
                "new-profile-slug"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
        verify(userRepository, never()).updateProfileById(any(), any(), any(), any());
        verifyNoInteractions(contentAuditService);
    }

    @Test
    @DisplayName("Should handle null values in update profile request")
    void updateProfile_WithNullValues_ShouldUpdateSuccessfully() throws UserNotFoundException {
        // Given
        Integer userId = 1;
        UpdateProfileRequest request = new UpdateProfileRequest(null, null, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.updateProfileById(null, null, null, userId)).thenReturn(1);

        // When
        boolean result = userService.updateProfile(userId, request);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).findById(userId);
        verify(userRepository).updateProfileById(null, null, null, userId);
        verify(contentAuditService).validate(request);
    }

    @Test
    @DisplayName("Should handle empty strings in update profile request")
    void updateProfile_WithEmptyStrings_ShouldUpdateSuccessfully() throws UserNotFoundException {
        // Given
        Integer userId = 1;
        UpdateProfileRequest request = new UpdateProfileRequest("", "", "");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.updateProfileById("", "", "", userId)).thenReturn(1);

        // When
        boolean result = userService.updateProfile(userId, request);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).findById(userId);
        verify(userRepository).updateProfileById("", "", "", userId);
        verify(contentAuditService).validate(request);
    }
}