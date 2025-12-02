package edu.xtu.bbs.user.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.validation.annotation.Validated;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "bbs.oss")
@Component
@Validated
public class OssConfiguration {
    @NotBlank
    private String secretId;

    @NotBlank
    private String secretKey;

    @NotBlank
    private String region;

    @Bean
    private COSCredentials cosCredentials() {
        return new BasicCOSCredentials(secretId, secretKey);
    }

    @Bean
    private ClientConfig config() {
        return new ClientConfig(new Region(region));
    }

    @Bean
    private COSClient cosClient(COSCredentials credentials, ClientConfig config) {
        return new COSClient(credentials, config);
    }

}
