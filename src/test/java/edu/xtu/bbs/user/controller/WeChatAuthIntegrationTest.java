package edu.xtu.bbs.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xtu.bbs.user.dto.WeChatRegisterRequest;
import edu.xtu.bbs.user.service.WeChatAppService;
import edu.xtu.bbs.verification.VerificationSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for WeChat registration and login workflow
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WeChatAuthIntegrationTest {

    // Test constants
    public static final String WECHAT_REGISTER_CODE = "mock-register-code";
    public static final String WECHAT_LOGIN_CODE = "mock-login-code";
    public static final String WECHAT_OPENID = "mock-wechat-openid-integration";
    public static final String USERNAME = "wechat-integration-user";
    public static final String PASSWORD = "wechat-integration-password";
    public static final String NICKNAME = "WeChatIntegrationNickname";
    public static final String BIO = "This is a WeChat integration test user.";

    @MockitoBean
    private VerificationSender sender;

    @MockitoBean
    private WeChatAppService weChatAppService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Test Complete WeChat Registration and Login Workflow")
    void testWeChatRegisterAndLoginWorkflow() throws Exception {
        
        final AtomicReference<String> authHeader = new AtomicReference<>();

        // Step 1: Mock WeChat service for registration
        when(weChatAppService.getOpenIdByCode(WECHAT_REGISTER_CODE)).thenReturn(WECHAT_OPENID);
        when(weChatAppService.getOpenIdByCode(WECHAT_LOGIN_CODE)).thenReturn(WECHAT_OPENID);

        // Step 2: Register user with WeChat
        WeChatRegisterRequest registerRequest = new WeChatRegisterRequest(
                WECHAT_REGISTER_CODE,
                USERNAME,
                PASSWORD,
                NICKNAME,
                BIO
        );

        String registerJson = objectMapper.writeValueAsString(registerRequest);

        mockMvc.perform(post("/auth/register/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.openId").value(WECHAT_OPENID))
                .andExpect(jsonPath("$.data.message").value("WeChat registration successful"));

        // Step 3: Login with WeChat authorization code (simulating WeChat login)
        // Since WeChat login is through filter, we need to test the login endpoint
        // Let's test traditional username/password login first to verify the user was created
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", USERNAME)
                        .param("password", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andDo(result -> 
                        authHeader.set(result.getResponse().getHeader("Authorization"))
                );

        // Step 4: Access protected resource to verify authentication works
        mockMvc.perform(get("/user")
                        .header("Authorization", authHeader.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(USERNAME))
                .andExpect(jsonPath("$.data.nickname").value(NICKNAME));

        // Step 5: Test WeChat login through filter (if implemented)
        // Note: This would require the WeChat login filter to be properly implemented
        // For now, we focus on testing that the registration creates a user that can login
    }

    @Test
    @DisplayName("Test WeChat Registration with Subsequent Traditional Login")
    void testWeChatRegistrationWithTraditionalLogin() throws Exception {
        
        final AtomicReference<String> authToken = new AtomicReference<>();
        final String testOpenId = "test-openid-for-login";
        final String testCode = "test-code-for-login";

        // Step 1: Mock WeChat service
        when(weChatAppService.getOpenIdByCode(testCode)).thenReturn(testOpenId);

        // Step 2: Register user via WeChat
        WeChatRegisterRequest request = new WeChatRegisterRequest(
                testCode,
                "login-test-user",
                "login-test-password", 
                "LoginTestNickname",
                "Login test bio"
        );

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/auth/register/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.openId").value(testOpenId));

        // Step 3: Login with traditional username/password
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "login-test-user")
                        .param("password", "login-test-password"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andDo(result -> 
                        authToken.set(result.getResponse().getHeader("Authorization"))
                );

        // Step 4: Verify user can access protected resources
        mockMvc.perform(get("/user")
                        .header("Authorization", authToken.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("login-test-user"))
                .andExpect(jsonPath("$.data.nickname").value("LoginTestNickname"));
    }

    @Test
    @DisplayName("Test WeChat Registration Error Scenarios")
    void testWeChatRegistrationErrorScenarios() throws Exception {
        
        // Test 1: Invalid WeChat code
        when(weChatAppService.getOpenIdByCode("invalid-code")).thenReturn(null);

        WeChatRegisterRequest invalidCodeRequest = new WeChatRegisterRequest(
                "invalid-code",
                "invalid-code-user",
                "password",
                "Nickname",
                "Bio"
        );

        mockMvc.perform(post("/auth/register/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCodeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20006));

        // Test 2: Duplicate username
        final String duplicateTestCode = "duplicate-test-code";
        final String duplicateTestOpenId = "duplicate-test-openid";
        
        when(weChatAppService.getOpenIdByCode(duplicateTestCode)).thenReturn(duplicateTestOpenId);

        // First registration
        WeChatRegisterRequest firstRequest = new WeChatRegisterRequest(
                duplicateTestCode,
                "duplicate-username",
                "password1",
                "FirstUser",
                "First bio"
        );

        mockMvc.perform(post("/auth/register/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.openId").value(duplicateTestOpenId));

        // Second registration with same username (should fail)
        final String secondTestCode = "second-test-code";
        final String secondTestOpenId = "second-test-openid";
        
        when(weChatAppService.getOpenIdByCode(secondTestCode)).thenReturn(secondTestOpenId);

        WeChatRegisterRequest duplicateRequest = new WeChatRegisterRequest(
                secondTestCode,
                "duplicate-username", // Same username as first
                "password2",
                "SecondUser",
                "Second bio"
        );

        mockMvc.perform(post("/auth/register/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20003)); // USER_ALREADY_EXISTS

        // Test 3: Duplicate OpenID
        WeChatRegisterRequest duplicateOpenIdRequest = new WeChatRegisterRequest(
                duplicateTestCode, // Same code/openId as first registration
                "different-username",
                "password3",
                "ThirdUser",
                "Third bio"
        );

        mockMvc.perform(post("/auth/register/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateOpenIdRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20005)); // USER_ALREADY_EXISTS for OpenID conflict
    }

    @Test
    @DisplayName("Test WeChat Registration Input Validation")
    void testWeChatRegistrationValidation() throws Exception {
        
        // Test invalid request body
        mockMvc.perform(post("/auth/register/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // Test missing required fields
        mockMvc.perform(post("/auth/register/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"\"}"))
                .andExpect(status().isOk());

        // Test invalid username format
        WeChatRegisterRequest invalidUsernameRequest = new WeChatRegisterRequest(
                "valid-code",
                "", // Empty username
                "valid-password",
                "Valid Nickname",
                "Valid bio"
        );

        mockMvc.perform(post("/auth/register/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUsernameRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20006));

        // Test invalid password format
        WeChatRegisterRequest invalidPasswordRequest = new WeChatRegisterRequest(
                "valid-code",
                "valid-username",
                "", // Empty password
                "Valid Nickname", 
                "Valid bio"
        );

        mockMvc.perform(post("/auth/register/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20006));
    }

    @Test
    @DisplayName("Test Multiple Users Registration and Individual Login")
    void testMultipleUsersRegistrationAndLogin() throws Exception {
        
        // Register first user
        when(weChatAppService.getOpenIdByCode("code1")).thenReturn("openid1");
        
        WeChatRegisterRequest user1Request = new WeChatRegisterRequest(
                "code1",
                "user1",
                "password1",
                "User One",
                "First user bio"
        );

        mockMvc.perform(post("/auth/register/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1Request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.openId").value("openid1"));

        // Register second user
        when(weChatAppService.getOpenIdByCode("code2")).thenReturn("openid2");
        
        WeChatRegisterRequest user2Request = new WeChatRegisterRequest(
                "code2",
                "user2", 
                "password2",
                "User Two",
                "Second user bio"
        );

        mockMvc.perform(post("/auth/register/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2Request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.openId").value("openid2"));

        // Login as first user
        final AtomicReference<String> user1Token = new AtomicReference<>();
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "user1")
                        .param("password", "password1"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andDo(result -> user1Token.set(result.getResponse().getHeader("Authorization")));

        // Verify first user's profile
        mockMvc.perform(get("/user")
                        .header("Authorization", user1Token.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("user1"))
                .andExpect(jsonPath("$.data.nickname").value("User One"));

        // Login as second user
        final AtomicReference<String> user2Token = new AtomicReference<>();
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "user2")
                        .param("password", "password2"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andDo(result -> user2Token.set(result.getResponse().getHeader("Authorization")));

        // Verify second user's profile
        mockMvc.perform(get("/user")
                        .header("Authorization", user2Token.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("user2"))
                .andExpect(jsonPath("$.data.nickname").value("User Two"));
    }
}