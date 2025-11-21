package edu.xtu.bbs.user.service;

import edu.xtu.bbs.user.config.WeChatAppConfiguration;
import edu.xtu.bbs.user.dto.WeChatAppSessionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test class for WeChatAppService.
 * <p>
 * This test class uses Mockito to mock the RestClient and test the
 * getOpenIdByCode method without making actual HTTP calls to WeChat API.
 * <p>
 * Tests cover:
 * - Successful API response with valid openId
 * - Null API response handling
 * - API response with null openId
 * - Verification of configuration method calls
 * - Constant values verification
 */
@ExtendWith(MockitoExtension.class)
class WeChatAppServiceTest {

    @Mock
    private WeChatAppConfiguration weChatAppConfiguration;

    @Mock
    private RestClient restClient;

    private WeChatAppService weChatAppService;

    @BeforeEach
    void setUp() {
        weChatAppService = new WeChatAppService(weChatAppConfiguration, restClient);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getOpenIdByCode_shouldReturnOpenId_whenWeChatApiReturnsValidResponse() {
        // Given
        String code = "test_code";
        String expectedOpenId = "test_open_id";
        String tokenUrl = "https://api.weixin.qq.com/sns/jscode2session";
        String appId = "test_app_id";
        String appSecret = "test_app_secret";

        when(weChatAppConfiguration.getTokenUrl()).thenReturn(tokenUrl);
        when(weChatAppConfiguration.getAppId()).thenReturn(appId);
        when(weChatAppConfiguration.getAppSecret()).thenReturn(appSecret);

        WeChatAppSessionDto mockResponse = new WeChatAppSessionDto(
                "session_key",
                "union_id",
                null,
                null,
                expectedOpenId
        );

        // Create mocks for the fluent API chain
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        // Mock the fluent chain
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(), any(), any())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(WeChatAppSessionDto.class)).thenReturn(mockResponse);

        // When
        // invoke
        String result = weChatAppService.getOpenIdByCode(code);

        // Then
        assertThat(result).isEqualTo(expectedOpenId);

        // Verify interactions
        verify(weChatAppConfiguration).getTokenUrl();
        verify(weChatAppConfiguration).getAppId();
        verify(weChatAppConfiguration).getAppSecret();
        verify(restClient).get();
        verify(responseSpec).body(WeChatAppSessionDto.class);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getOpenIdByCode_shouldReturnNull_whenWeChatApiReturnsNullResponse() {
        // Given
        String code = "test_code";
        String tokenUrl = "https://api.weixin.qq.com/sns/jscode2session";
        String appId = "test_app_id";
        String appSecret = "test_app_secret";

        when(weChatAppConfiguration.getTokenUrl()).thenReturn(tokenUrl);
        when(weChatAppConfiguration.getAppId()).thenReturn(appId);
        when(weChatAppConfiguration.getAppSecret()).thenReturn(appSecret);

        // Create mocks for the fluent API chain
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        // Mock the fluent chain to return null
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(), any(), any())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(WeChatAppSessionDto.class)).thenReturn(null);

        // When
        String result = weChatAppService.getOpenIdByCode(code);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getOpenIdByCode_shouldReturnNull_whenSessionDtoHasNullOpenId() {
        // Given
        String code = "test_code";
        String tokenUrl = "https://api.weixin.qq.com/sns/jscode2session";
        String appId = "test_app_id";
        String appSecret = "test_app_secret";

        when(weChatAppConfiguration.getTokenUrl()).thenReturn(tokenUrl);
        when(weChatAppConfiguration.getAppId()).thenReturn(appId);
        when(weChatAppConfiguration.getAppSecret()).thenReturn(appSecret);

        WeChatAppSessionDto mockResponse = new WeChatAppSessionDto(
            "session_key",
            "union_id",
            null, // no error code
            null,
            null // null openid
        );

        // Create mocks for the fluent API chain
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        // Mock the fluent chain
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(), any(), any())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(WeChatAppSessionDto.class)).thenReturn(mockResponse);

        // When
        String result = weChatAppService.getOpenIdByCode(code);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void getOpenIdByCode_shouldVerifyCorrectDefaultGrantType() {
        // Given & When & Then
        assertThat(WeChatAppService.DEFAULT_GRANT_TYPE).isEqualTo("authorization_code");
    }

    @Test
    void constructor_shouldSetFieldsCorrectly() {
        // Given & When
        WeChatAppService service = new WeChatAppService(weChatAppConfiguration, restClient);

        // Then
        assertThat(ReflectionTestUtils.getField(service, "weChatAppConfiguration")).isEqualTo(weChatAppConfiguration);
        assertThat(ReflectionTestUtils.getField(service, "restClient")).isEqualTo(restClient);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getOpenIdByCode_shouldThrowIllegalArgument_forErrcode40029() {
        String code = "c1";
        when(weChatAppConfiguration.getTokenUrl()).thenReturn("u");
        when(weChatAppConfiguration.getAppId()).thenReturn("a");
        when(weChatAppConfiguration.getAppSecret()).thenReturn("s");

        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(), any(), any())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(WeChatAppSessionDto.class)).thenReturn(new WeChatAppSessionDto("k", "u", 40029, "err", "oid"));

        var ex = assertThrows(IllegalArgumentException.class, () -> weChatAppService.getOpenIdByCode(code));
        assertThat(ex.getMessage()).isEqualTo("Invalid code");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getOpenIdByCode_shouldThrowIllegalArgument_forErrcode40226() {
        String code = "c2";
        when(weChatAppConfiguration.getTokenUrl()).thenReturn("u");
        when(weChatAppConfiguration.getAppId()).thenReturn("a");
        when(weChatAppConfiguration.getAppSecret()).thenReturn("s");

        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(), any(), any())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(WeChatAppSessionDto.class)).thenReturn(new WeChatAppSessionDto("k", "u", 40226, "err", "oid"));

        var ex = assertThrows(IllegalArgumentException.class, () -> weChatAppService.getOpenIdByCode(code));
        assertThat(ex.getMessage()).isEqualTo("Code block");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getOpenIdByCode_shouldThrowIllegalState_forErrcode45011_andMinus1_andDefault() {
        when(weChatAppConfiguration.getTokenUrl()).thenReturn("u");
        when(weChatAppConfiguration.getAppId()).thenReturn("a");
        when(weChatAppConfiguration.getAppSecret()).thenReturn("s");

        // 45011 -> rate limit
        {
            RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
            when(restClient.get()).thenReturn(uriSpec);
            when(uriSpec.uri(anyString(), any(), any(), any())).thenReturn(uriSpec);
            when(uriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(WeChatAppSessionDto.class)).thenReturn(new WeChatAppSessionDto("k", "u", 45011, "err", "oid"));
            var ex = assertThrows(IllegalStateException.class, () -> weChatAppService.getOpenIdByCode("c3"));
            assertThat(ex.getMessage()).isEqualTo("Rate limit exceeded");
        }

        // -1 -> system busy
        {
            RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
            when(restClient.get()).thenReturn(uriSpec);
            when(uriSpec.uri(anyString(), any(), any(), any())).thenReturn(uriSpec);
            when(uriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(WeChatAppSessionDto.class)).thenReturn(new WeChatAppSessionDto("k", "u", -1, "err", "oid"));
            var ex = assertThrows(IllegalStateException.class, () -> weChatAppService.getOpenIdByCode("c4"));
            assertThat(ex.getMessage()).isEqualTo("System busy");
        }

        // default -> unknown
        {
            RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
            when(restClient.get()).thenReturn(uriSpec);
            when(uriSpec.uri(anyString(), any(), any(), any())).thenReturn(uriSpec);
            when(uriSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(WeChatAppSessionDto.class)).thenReturn(new WeChatAppSessionDto("k", "u", 12345, "err", "oid"));
            var ex = assertThrows(IllegalStateException.class, () -> weChatAppService.getOpenIdByCode("c5"));
            assertThat(ex.getMessage()).contains("Unknown error code: 12345");
        }
    }

    @Test
    void getOpenIdByCode_shouldReturnNull_whenRestClientThrows() {
        when(weChatAppConfiguration.getTokenUrl()).thenReturn("u");
        when(weChatAppConfiguration.getAppId()).thenReturn("a");
        when(weChatAppConfiguration.getAppSecret()).thenReturn("s");

        when(restClient.get()).thenThrow(new org.springframework.web.client.RestClientException("boom"));

        String result = weChatAppService.getOpenIdByCode("any");
        assertThat(result).isNull();
    }
}