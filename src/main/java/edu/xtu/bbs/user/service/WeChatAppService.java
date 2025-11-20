package edu.xtu.bbs.user.service;

import edu.xtu.bbs.user.config.WeChatAppConfiguration;
import edu.xtu.bbs.user.dto.WeChatAppSessionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
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
        WeChatAppSessionDto sessionDto;
        try {
            sessionDto = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(tokenUrl)
                            .queryParam("appid", appId)
                            .queryParam("secret", appSecret)
                            .queryParam("js_code", code)
                            .queryParam("grant_type", DEFAULT_GRANT_TYPE)
                            .build())
                    .retrieve()
                    .body(WeChatAppSessionDto.class);
        } catch (IllegalArgumentException e) {
            log.warn(e.getMessage(), e);
            return null;
        }


        if (sessionDto != null) {
            return sessionDto.openid();
        }
        return null;
    }

}
