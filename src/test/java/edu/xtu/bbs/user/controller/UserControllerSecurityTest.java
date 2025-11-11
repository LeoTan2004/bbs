package edu.xtu.bbs.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xtu.bbs.user.dto.UpdateProfileRequest;
import edu.xtu.bbs.user.exception.IllegalContentTypeException;
import edu.xtu.bbs.user.model.User;
import edu.xtu.bbs.user.service.AuthenticationService;
import edu.xtu.bbs.user.service.AvatarService;
import edu.xtu.bbs.user.service.UserService;
import edu.xtu.bbs.user.vo.UploadRequest;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.unit.DataSize;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerSecurityTest {

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_NICKNAME = "Test User";
    private static final String TEST_BIO = "This is a test user bio.";
    private static final String TEST_PROFILE_SLUG = "test-user-slug";
    private static final String ANOTHER_USERNAME = "anotheruser";
    private static final String UPLOAD_URL = "https://example.com/upload/avatar";
    private static final String CONTENT_TYPE = "image/jpeg";
    private static final Long FILE_SIZE = 1024L;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AvatarService avatarService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = createMockUser(TEST_NICKNAME, TEST_BIO, TEST_PROFILE_SLUG);
    }

    private User createMockUser(String nickname, String bio, String profileSlug) {
        User user = new User();
        user.setId(1);
        user.setUsername(UserControllerSecurityTest.TEST_USERNAME);
        user.setNickname(nickname);
        user.setBio(bio);
        user.setProfileSlug(profileSlug);
        user.setPasswordHash("hashedPassword");
        return user;
    }


    @Test
    @DisplayName("Test Get Current User - With Mock User Annotation")
    @WithMockUser(username = TEST_USERNAME)
    void testGetCurrentUserWithAnnotation() throws Exception {
        when(authenticationService.getCurrentUser()).thenReturn(mockUser);

        mockMvc.perform(get("/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.data.nickname").value(TEST_NICKNAME));
    }

    @Test
    @DisplayName("Test Get User By Username - With Authentication")
    @WithMockUser(username = TEST_USERNAME)
    void testGetUserByUsernameWithAuth() throws Exception {
        when(userService.findByUsername(TEST_USERNAME)).thenReturn(mockUser);

        mockMvc.perform(get("/user/{username}", TEST_USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.data.nickname").value(TEST_NICKNAME));
    }


    @Test
    @DisplayName("Test Get Avatar Upload URL - Success with Request Post Processor")
    void testGetAvatarUploadUrlSuccessWithRequestProcessor() throws Exception {
        UploadRequest request = new UploadRequest(CONTENT_TYPE, FILE_SIZE);

        when(authenticationService.getCurrentUser()).thenReturn(mockUser);
        when(avatarService.generateAvatarUploadUrl(eq(TEST_USERNAME), eq(CONTENT_TYPE), eq(DataSize.ofBytes(FILE_SIZE))))
                .thenReturn(UPLOAD_URL);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/user/{username}/avatar", TEST_USERNAME)
                        .with(user(mockUser)) // Use Spring Security Test's user() method
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string(UPLOAD_URL));
    }

    @Test
    @DisplayName("Test Get Avatar Upload URL - Security Exception with Different User")
    void testGetAvatarUploadUrlSecurityExceptionDifferentUser() throws Exception {
        UploadRequest request = new UploadRequest(CONTENT_TYPE, FILE_SIZE);

        // Mock current user as mockUser, but try to upload avatar for another user
        when(authenticationService.getCurrentUser()).thenReturn(mockUser);

        String requestJson = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/user/{username}/avatar", ANOTHER_USERNAME)
                        .with(user(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Test Update Profile - Success with Authentication")
    void testUpdateProfileSuccessWithAuth() throws Exception {
        String updatedNickname = "Updated Nickname";
        String updatedBio = "Updated bio description";
        String updatedProfileSlug = "updated-slug";

        UpdateProfileRequest request = new UpdateProfileRequest(updatedNickname, updatedBio, updatedProfileSlug);

        User updatedUser = createMockUser(updatedNickname, updatedBio, updatedProfileSlug);

        when(authenticationService.getCurrentUser()).thenReturn(mockUser);
        when(userService.updateProfile(eq(mockUser.getId()), any(UpdateProfileRequest.class))).thenReturn(true);
        when(userService.findByUsername(TEST_USERNAME)).thenReturn(updatedUser);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(patch("/user/{username}/profile", TEST_USERNAME)
                        .with(user(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.data.nickname").value(updatedNickname))
                .andExpect(jsonPath("$.data.bio").value(updatedBio))
                .andExpect(jsonPath("$.data.profileSlug").value(updatedProfileSlug));
    }

    @Test
    @DisplayName("Test Update Profile - Security Exception with Different User")
    void testUpdateProfileSecurityExceptionDifferentUser() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Updated Nickname", "Updated bio", "updated-slug");

        when(authenticationService.getCurrentUser()).thenReturn(mockUser);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(patch("/user/{username}/profile", ANOTHER_USERNAME)
                        .with(user(mockUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    // ========== Test unauthenticated user access ==========

    @Test
    @DisplayName("Test Get Current User - Unauthenticated")
    void testGetCurrentUserUnauthenticated() throws Exception {
        // Don't provide any authentication information
        mockMvc.perform(get("/user"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test Get Avatar Upload URL - Unauthenticated")
    void testGetAvatarUploadUrlUnauthenticated() throws Exception {
        UploadRequest request = new UploadRequest(CONTENT_TYPE, FILE_SIZE);
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/user/{username}/avatar", TEST_USERNAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden()); // Spring Security returns 403 for unauthenticated requests
    }

    @Test
    @DisplayName("Test Update Profile - Unauthenticated")
    void testUpdateProfileUnauthenticated() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Updated Nickname", "Updated bio", "updated-slug");
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(patch("/user/{username}/profile", TEST_USERNAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden()); // Spring Security returns 403 for unauthenticated requests
    }

    // ========== Test exception scenarios ==========

    @Test
    @DisplayName("Test Get Avatar Upload URL - File Size Limit Exceeded")
    @WithMockUser(username = TEST_USERNAME)
    void testGetAvatarUploadUrlFileSizeLimitExceeded() throws Exception {
        UploadRequest request = new UploadRequest(CONTENT_TYPE, FILE_SIZE);

        when(authenticationService.getCurrentUser()).thenReturn(mockUser);
        when(avatarService.generateAvatarUploadUrl(eq(TEST_USERNAME), eq(CONTENT_TYPE), eq(DataSize.ofBytes(FILE_SIZE))))
                .thenThrow(new FileSizeLimitExceededException("File size limit exceeded", FILE_SIZE, 512L));

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/user/{username}/avatar", TEST_USERNAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk()) // Global exception handler returns 200
                .andExpect(jsonPath("$.code").value(90004)) // Check error code
                .andExpect(jsonPath("$.message").value("File size exceeds limit"));
    }

    @Test
    @DisplayName("Test Get Avatar Upload URL - Illegal Content Type")
    @WithMockUser(username = TEST_USERNAME)
    void testGetAvatarUploadUrlIllegalContentType() throws Exception {
        String invalidContentType = "application/pdf";
        UploadRequest request = new UploadRequest(invalidContentType, FILE_SIZE);

        when(authenticationService.getCurrentUser()).thenReturn(mockUser);
        when(avatarService.generateAvatarUploadUrl(eq(TEST_USERNAME), eq(invalidContentType), eq(DataSize.ofBytes(FILE_SIZE))))
                .thenThrow(new IllegalContentTypeException(invalidContentType, "Invalid content type"));

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/user/{username}/avatar", TEST_USERNAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk()) // Global exception handler returns 200
                .andExpect(jsonPath("$.code").value(90004)) // Check error code
                .andExpect(jsonPath("$.message").value("Unsupported file type"));
    }

    @Test
    @DisplayName("Test Get User By Username - User Not Found")
    @WithMockUser(username = TEST_USERNAME)
    void testGetUserByUsernameNotFound() throws Exception {
        when(userService.findByUsername("nonexistent")).thenThrow(new UsernameNotFoundException("User not found"));

        mockMvc.perform(get("/user/{username}", "nonexistent"))
                .andExpect(status().isUnauthorized()) // AuthenticationException -> 401
                .andExpect(jsonPath("$.code").value(10002)); // UNAUTHORIZED error code
    }

    @Test
    @DisplayName("Test Update Profile - Update Failed")
    @WithMockUser(username = TEST_USERNAME)
    void testUpdateProfileFailed() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Updated Nickname", "Updated bio", "updated-slug");

        when(authenticationService.getCurrentUser()).thenReturn(mockUser);
        when(userService.updateProfile(eq(mockUser.getId()), any(UpdateProfileRequest.class))).thenReturn(false);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(patch("/user/{username}/profile", TEST_USERNAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isInternalServerError()); // RuntimeException -> 500
    }
}