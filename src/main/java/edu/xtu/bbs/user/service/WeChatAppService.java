package edu.xtu.bbs.user.service;

import edu.xtu.bbs.user.config.WeChatAppConfiguration;
import edu.xtu.bbs.user.dto.WeChatAppSessionDto;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class WeChatAppService {

    private final WeChatAppConfiguration weChatAppConfiguration;
    private final RestClient restClient;

    public static final String DEFAULT_GRANT_TYPE = "authorization_code";

    public WeChatAppService(WeChatAppConfiguration weChatAppConfiguration, RestClient restClient) {
        this.weChatAppConfiguration = weChatAppConfiguration;
        this.restClient = restClient;
    }

    public String getOpenIdByCode(String code) {
        final String tokenUrl = weChatAppConfiguration.getTokenUrl();
        final String appId = weChatAppConfiguration.getAppId();
        final String appSecret = weChatAppConfiguration.getAppSecret();

        final WeChatAppSessionDto sessionDto = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(tokenUrl)
                        .queryParam("appid", appId)
                        .queryParam("secret", appSecret)
                        .queryParam("js_code", code)
                        .queryParam("grant_type", DEFAULT_GRANT_TYPE)
                        .build())
                .retrieve()
                .body(WeChatAppSessionDto.class);

        if (sessionDto != null) {
            return sessionDto.openid();
        }
        return null;
    }

}
