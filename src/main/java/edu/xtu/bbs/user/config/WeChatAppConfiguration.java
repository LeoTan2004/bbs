package edu.xtu.bbs.user.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@ConfigurationProperties(prefix = "bbs.wechat.app")
@Data
@Configuration
public class WeChatAppConfiguration {

    @NotBlank
    private String appId;

    @NotBlank
    private String appSecret;

    private String tokenUrl = "https://api.weixin.qq.com/sns/jscode2session";

    @Bean
    RestClient restClient() {
        return RestClient.create();
    }

}
