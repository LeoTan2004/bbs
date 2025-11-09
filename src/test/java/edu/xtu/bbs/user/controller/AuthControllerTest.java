package edu.xtu.bbs.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xtu.bbs.user.dto.CreateUserRequest;
import edu.xtu.bbs.user.service.AuthenticationService;
import edu.xtu.bbs.user.vo.PasswordUpdateRequest;
import edu.xtu.bbs.user.vo.RegisterVo;
import edu.xtu.bbs.verification.VerificationParam;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    public static final String EMAIL = "123456789@example.com";
    public static final String RESET_EMAIL = "reset.password@example.com";
    public static final String USERNAME = "mock-user";
    public static final String RESET_USERNAME = "reset-user";
    public static final String PASSWORD = "mock-password";
    public static final String NEW_PASSWORD = "new-mock-password";
    public static final String NICKNAME = "MockNickname";
    public static final String RESET_NICKNAME = "ResetNickname";
    public static final String BIO = "This is a mock user.";
    @MockitoBean
    private VerificationSender sender;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @DisplayName("Test Register By Email")
    void testRegisterByEmail() throws Exception {

        final AtomicReference<String> code = new AtomicReference<>();
        final AtomicReference<String> token = new AtomicReference<>();
        final String pattern = "code is: 【";

        when(sender.sendCode(eq(EMAIL), any())).then(
                invocation -> {
                    final String verificationInfo = invocation.getArgument(1).toString();
                    final int startIdx = verificationInfo.indexOf(pattern) + pattern.length();
                    final String verifyCode = verificationInfo.substring(startIdx, startIdx + 6);
                    code.set(verifyCode);
                    return true;
                }
        );

        // Step 1: Send verification code to email
        mockMvc.perform(post("/auth/register/send-code")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", EMAIL)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists())
                .andDo(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    // Parse JSON to get token from VO object
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jsonNode = mapper.readTree(responseBody);
                    token.set(jsonNode.get("data").get("token").asText());
                });

        final RegisterVo registerVo = new RegisterVo();
        registerVo.setUser(new CreateUserRequest(USERNAME, PASSWORD, EMAIL, NICKNAME, BIO));
        registerVo.setVerification(new VerificationParam(EMAIL, token.get(), AuthenticationService.BIND_EMAIL, code.get()));

        final String registerJson = objectMapper.writeValueAsString(registerVo);

        // Step 2: Register with the received code and token
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(EMAIL));

        final AtomicReference<String> authHeader = new AtomicReference<>();

        // Step 3: Login with the registered email to verify registration success
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", USERNAME)
                        .param("password", PASSWORD)
                )
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andDo(
                        result ->
                                authHeader.set(result.getResponse().getHeader("Authorization"))
                );

        // Step 4: Access a protected resource to confirm authentication
        mockMvc.perform(get("/user")
                        .header("Authorization", authHeader.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(USERNAME));


    }

    @Test
    @DisplayName("Test Password Reset After Registration")
    void testPasswordResetAfterRegistration() throws Exception {
        
        final AtomicReference<String> registerCode = new AtomicReference<>();
        final AtomicReference<String> registerToken = new AtomicReference<>();
        final AtomicReference<String> resetCode = new AtomicReference<>();
        final AtomicReference<String> resetToken = new AtomicReference<>();
        final String pattern = "code is: 【";

        when(sender.sendCode(eq(RESET_EMAIL), any())).then(
                invocation -> {
                    final String verificationInfo = invocation.getArgument(1).toString();
                    final int startIdx = verificationInfo.indexOf(pattern) + pattern.length();
                    final String verifyCode = verificationInfo.substring(startIdx, startIdx + 6);
                    
                    // Determine if this is for registration or password reset based on the context
                    if (registerCode.get() == null) {
                        registerCode.set(verifyCode);
                    } else {
                        resetCode.set(verifyCode);
                    }
                    return true;
                }
        );

        // Step 1: Register a user first (reusing the registration process)
        mockMvc.perform(post("/auth/register/send-code")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", RESET_EMAIL)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists())
                .andDo(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jsonNode = mapper.readTree(responseBody);
                    registerToken.set(jsonNode.get("data").get("token").asText());
                });

        final RegisterVo registerVo = new RegisterVo();
        registerVo.setUser(new CreateUserRequest(RESET_USERNAME, PASSWORD, RESET_EMAIL, RESET_NICKNAME, BIO));
        registerVo.setVerification(new VerificationParam(RESET_EMAIL, registerToken.get(), AuthenticationService.BIND_EMAIL, registerCode.get()));

        final String registerJson = objectMapper.writeValueAsString(registerVo);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(RESET_EMAIL));

        // Step 2: Send verification code for password reset
        mockMvc.perform(post("/auth/reset-password/send-code")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", RESET_EMAIL)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists())
                .andDo(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jsonNode = mapper.readTree(responseBody);
                    resetToken.set(jsonNode.get("data").get("token").asText());
                });

        // Step 3: Reset password with the received code and token
        final PasswordUpdateRequest passwordUpdateRequest = new PasswordUpdateRequest();
        passwordUpdateRequest.setEmail(RESET_EMAIL);
        passwordUpdateRequest.setPassword(NEW_PASSWORD);
        passwordUpdateRequest.setVerification(new VerificationParam(RESET_EMAIL, resetToken.get(), AuthenticationService.CHANGE_PWD, resetCode.get()));

        final String resetPasswordJson = objectMapper.writeValueAsString(passwordUpdateRequest);

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetPasswordJson)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true));

        final AtomicReference<String> authHeader = new AtomicReference<>();

        // Step 4: Login with the new password to verify reset success
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", RESET_USERNAME)
                        .param("password", NEW_PASSWORD)
                )
                .andExpect(status().isOk())
                .andExpect(header().exists("Authorization"))
                .andDo(
                        result ->
                                authHeader.set(result.getResponse().getHeader("Authorization"))
                );

        // Step 5: Verify the old password no longer works
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", RESET_USERNAME)
                        .param("password", PASSWORD)  // Old password
                )
                .andExpect(status().isUnauthorized());

        // Step 6: Access a protected resource with new authentication to confirm password change
        mockMvc.perform(get("/user")
                        .header("Authorization", authHeader.get()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(RESET_USERNAME));
    }

}