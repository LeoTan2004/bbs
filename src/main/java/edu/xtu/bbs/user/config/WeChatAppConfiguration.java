package edu.xtu.bbs.user.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.validation.annotation.Validated;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "bbs.wechat.app")
@Data
@Configuration
@Validated
public class WeChatAppConfiguration {

    @NotBlank
    private String appId;

    @NotBlank
    private String appSecret;

    private String tokenUrl = "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={jsCode}&grant_type=authorization_code";

    @Bean
    public RestClient restClient(RestClient.Builder builder) {

        final MappingJackson2HttpMessageConverter jacksonConverter =
                new MappingJackson2HttpMessageConverter();

        final List<MediaType> mediaTypes = new ArrayList<>(jacksonConverter.getSupportedMediaTypes());
        mediaTypes.add(MediaType.TEXT_PLAIN);
        mediaTypes.add(MediaType.TEXT_HTML);
        jacksonConverter.setSupportedMediaTypes(mediaTypes);

        return builder
                .messageConverters(converters -> {
                    converters.add(jacksonConverter);
                })
                .build();
    }

}
