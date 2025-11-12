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
 * Complete WeChat authentication workflow test (registration + login)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WeChatCompleteWorkflowTest {

    // Test constants
    public static final String WECHAT_REGISTER_CODE = "register-code";
    public static final String WECHAT_LOGIN_CODE = "login-code";  
    public static final String WECHAT_OPENID = "workflow-test-openid";
    public static final String USERNAME = "wechat-workflow-user";
    public static final String PASSWORD = "wechat-workflow-password";
    public static final String NICKNAME = "WeChatWorkflowUser";
    public static final String BIO = "WeChat workflow test user";

    @MockitoBean
    private VerificationSender sender;

    @MockitoBean
    private WeChatAppService weChatAppService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Test Complete WeChat Authentication Workflow: Register → Traditional Login → WeChat Login")
    void testCompleteWeChatAuthenticationWorkflow() throws Exception {
        
        final AtomicReference<String> authTokenFromTraditionalLogin = new AtomicReference<>();
        final AtomicReference<String> authTokenFromWeChatLogin = new AtomicReference<>();

        // Step 1: Mock WeChat service for both registration and login
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

        // Step 3: Login with traditional username/password authentication
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", USERNAME)
                        .param("password", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andDo(result -> 
                        authTokenFromTraditionalLogin.set(result.getResponse().getHeader("Authorization"))
                );

        // Step 4: Verify traditional login worked - access protected resource
        mockMvc.perform(get("/user")
                        .header("Authorization", authTokenFromTraditionalLogin.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(USERNAME))
                .andExpect(jsonPath("$.data.nickname").value(NICKNAME));

        // Step 5: Login with WeChat authentication (using the same OpenID that was registered)
        mockMvc.perform(post("/auth/login-with-wechat")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("code", WECHAT_LOGIN_CODE))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andDo(result ->
                        authTokenFromWeChatLogin.set(result.getResponse().getHeader("Authorization"))
                );

        // Step 6: Verify WeChat login worked - access protected resource with WeChat-generated token
        mockMvc.perform(get("/user")
                        .header("Authorization", authTokenFromWeChatLogin.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(USERNAME))
                .andExpect(jsonPath("$.data.nickname").value(NICKNAME));
    }

    @Test
    @DisplayName("Test WeChat Login with Non-Existent OpenID")
    void testWeChatLoginWithNonExistentOpenID() throws Exception {
        // Mock WeChat service to return an OpenID that hasn't been registered
        when(weChatAppService.getOpenIdByCode("non-existent-code")).thenReturn("non-existent-openid");

        // Attempt to login with WeChat using non-existent OpenID - should fail
        mockMvc.perform(post("/auth/login-with-wechat")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("code", "non-existent-code"))
                .andExpect(status().isUnauthorized()); // Should fail authentication
    }

    @Test
    @DisplayName("Test WeChat Login with Invalid Code")
    void testWeChatLoginWithInvalidCode() throws Exception {
        // Mock WeChat service to return null for invalid code
        when(weChatAppService.getOpenIdByCode("invalid-wechat-code")).thenReturn(null);

        // Attempt to login with invalid WeChat code - should fail
        mockMvc.perform(post("/auth/login-with-wechat")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("code", "invalid-wechat-code"))
                .andExpect(status().isUnauthorized()); // Should fail authentication
    }

    @Test
    @DisplayName("Test Multiple WeChat Users Login")
    void testMultipleWeChatUsersLogin() throws Exception {
        // Register and login with first WeChat user
        when(weChatAppService.getOpenIdByCode("user1-register-code")).thenReturn("user1-openid");
        when(weChatAppService.getOpenIdByCode("user1-login-code")).thenReturn("user1-openid");
        
        WeChatRegisterRequest user1Request = new WeChatRegisterRequest(
                "user1-register-code",
                "wechat-user1",
                "password1",
                "WeChat User 1",
                "First WeChat user"
        );

        mockMvc.perform(post("/auth/register/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1Request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.openId").value("user1-openid"));

        // Register and login with second WeChat user
        when(weChatAppService.getOpenIdByCode("user2-register-code")).thenReturn("user2-openid");
        when(weChatAppService.getOpenIdByCode("user2-login-code")).thenReturn("user2-openid");
        
        WeChatRegisterRequest user2Request = new WeChatRegisterRequest(
                "user2-register-code", 
                "wechat-user2",
                "password2",
                "WeChat User 2",
                "Second WeChat user"
        );

        mockMvc.perform(post("/auth/register/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2Request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.openId").value("user2-openid"));

        // Login as user 1 via WeChat
        final AtomicReference<String> user1Token = new AtomicReference<>();
        mockMvc.perform(post("/auth/login-with-wechat")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("code", "user1-login-code"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andDo(result -> user1Token.set(result.getResponse().getHeader("Authorization")));

        // Login as user 2 via WeChat
        final AtomicReference<String> user2Token = new AtomicReference<>();
        mockMvc.perform(post("/auth/login-with-wechat")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("code", "user2-login-code"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andDo(result -> user2Token.set(result.getResponse().getHeader("Authorization")));

        // Verify both users can access their profiles independently
        mockMvc.perform(get("/user")
                        .header("Authorization", user1Token.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("wechat-user1"))
                .andExpect(jsonPath("$.data.nickname").value("WeChat User 1"));

        mockMvc.perform(get("/user") 
                        .header("Authorization", user2Token.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("wechat-user2"))
                .andExpect(jsonPath("$.data.nickname").value("WeChat User 2"));
    }
}