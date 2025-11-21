package edu.xtu.bbs.user.service;

import edu.xtu.bbs.user.config.WeChatAppConfiguration;
import edu.xtu.bbs.user.dto.WeChatAppSessionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
                    .uri(tokenUrl, appId, appSecret, code)
                    .retrieve()
                    .body(WeChatAppSessionDto.class);
        } catch (IllegalArgumentException | RestClientException e) {
            log.warn(e.getMessage(), e);
            return null;
        }

        if (sessionDto == null) {
            return null;
        }
        if (sessionDto.errcode() == null) {
            return sessionDto.openid();
        }
        switch (sessionDto.errcode()) {
            case 40029 -> {
                log.debug("WeChatAppService.getOpenIdByCode: invalid code");
                throw new IllegalArgumentException("Invalid code");
            }
            case 45011 -> {
                log.warn("WeChatAppService.getOpenIdByCode: rate limit exceeded");
                throw new IllegalStateException("Rate limit exceeded");
            }
            case 40226 -> {
                log.debug("WeChatAppService.getOpenIdByCode: code block");
                throw new IllegalArgumentException("Code block");
            }
            case -1 -> {
                log.warn("WeChatAppService.getOpenIdByCode: system busy");
                throw new IllegalStateException("System busy");
            }
            default -> {
                log.warn("WeChatAppService.getOpenIdByCode: unknown error code {}", sessionDto.errcode());
                throw new IllegalStateException("Unknown error code: " + sessionDto.errcode());
            }
        }
    }

}
