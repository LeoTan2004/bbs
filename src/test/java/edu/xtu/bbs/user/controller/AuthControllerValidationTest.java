package edu.xtu.bbs.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.xtu.bbs.common.exception.ContentAuditViolationException;
import edu.xtu.bbs.common.response.ResponseCode;
import edu.xtu.bbs.user.dto.CreateUserRequest;
import edu.xtu.bbs.user.service.AuthenticationService;
import edu.xtu.bbs.user.service.UserBinderService;
import edu.xtu.bbs.user.vo.RegisterVo;
import edu.xtu.bbs.verification.VerificationParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = edu.xtu.bbs.user.filter.JwtAuthorizationFilter.class),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "edu\\.xtu\\.bbs\\.user\\.filter\\..*")
        }
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private UserBinderService userBinderService;

    @Test
    @DisplayName("register/send-code should reject empty email")
    void registerSendCode_ShouldReturnBadRequest_WhenEmailEmpty() throws Exception {
        mockMvc.perform(post("/auth/register/send-code")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResponseCode.PARAM_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.PARAM_INVALID.getMessage()));

        verify(authenticationService, never()).bindEmailVerify(anyString());
    }

    @Test
    @DisplayName("register/send-code should reject invalid email format")
    void registerSendCode_ShouldReturnBadRequest_WhenEmailInvalid() throws Exception {
        mockMvc.perform(post("/auth/register/send-code")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "not-an-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResponseCode.PARAM_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.PARAM_INVALID.getMessage()));

        verify(authenticationService, never()).bindEmailVerify(anyString());
    }

    @Test
    @DisplayName("register should return PARAM_INVALID when sensitive content detected")
    void register_ShouldReturnParamInvalid_WhenContentAuditFails() throws Exception {
        RegisterVo registerVo = new RegisterVo();
        registerVo.setUser(new CreateUserRequest(
                "audit-user",
                "password123",
                "audit@example.com",
                "Sensitive Nickname",
                "About me"
        ));
        registerVo.setVerification(new VerificationParam(
                "audit@example.com",
                "token-123",
                AuthenticationService.BIND_EMAIL,
                "654321"
        ));

        when(authenticationService.register(any(), any()))
                .thenThrow(new ContentAuditViolationException("nickname", "Content contains sensitive expressions."));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerVo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseCode.PARAM_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value("Content contains sensitive expressions."))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
