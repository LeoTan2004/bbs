package edu.xtu.bbs.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xtu.bbs.user.dto.CreateUserRequest;
import edu.xtu.bbs.user.service.AuthenticationService;
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
    public static final String USERNAME = "mock-user";
    public static final String PASSWORD = "mock-password";
    public static final String NICKNAME = "MockNickname";
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
                .andDo(result ->
                        token.set(result.getResponse().getContentAsString())
                );

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
                .andExpect(content().string(EMAIL));

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
                .andExpect(jsonPath("$.username").value(USERNAME));


    }

}