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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WeChatAuthControllerTest {

    public static final String WECHAT_CODE = "mock-wechat-code";
    public static final String WECHAT_OPENID = "mock-wechat-openid";
    public static final String USERNAME = "wechat-user";
    public static final String PASSWORD = "wechat-password";
    public static final String NICKNAME = "WeChatNickname";
    public static final String BIO = "This is a WeChat user.";

    @MockitoBean
    private VerificationSender sender;

    @MockitoBean
    private WeChatAppService weChatAppService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Test WeChat Register")
    void testWeChatRegister() throws Exception {
        // Mock WeChat service to return OpenID
        when(weChatAppService.getOpenIdByCode(WECHAT_CODE)).thenReturn(WECHAT_OPENID);

        WeChatRegisterRequest request = new WeChatRegisterRequest(
                WECHAT_CODE,
                USERNAME,
                PASSWORD,
                NICKNAME,
                BIO
        );

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/auth/register/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.openId").value(WECHAT_OPENID))
                .andExpect(jsonPath("$.data.message").value("WeChat registration successful"));
    }

    @Test
    @DisplayName("Test WeChat Register with Invalid Request")
    void testWeChatRegisterWithInvalidRequest() throws Exception {
        mockMvc.perform(post("/auth/register/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Test WeChat Register with Existing Username")
    void testWeChatRegisterWithExistingUsername() throws Exception {
        // Mock WeChat service to return OpenID
        when(weChatAppService.getOpenIdByCode(WECHAT_CODE)).thenReturn(WECHAT_OPENID);

        // First register a user with regular registration to occupy the username
        // (This would need to be set up in the test data or mock properly)

        WeChatRegisterRequest request = new WeChatRegisterRequest(
                WECHAT_CODE,
                "existing-username", // This should be an already existing username
                PASSWORD,
                NICKNAME,
                BIO
        );

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/auth/register/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20003)); // USER_ALREADY_EXISTS
    }

    @Test
    @DisplayName("Test WeChat Register with Invalid Code")
    void testWeChatRegisterWithInvalidCode() throws Exception {
        // Mock WeChat service to return null/empty OpenID (invalid code)
        when(weChatAppService.getOpenIdByCode("invalid-code")).thenReturn(null);

        WeChatRegisterRequest request = new WeChatRegisterRequest(
                "invalid-code",
                "test-user",
                PASSWORD,
                NICKNAME,
                BIO
        );

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/auth/register/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }
}