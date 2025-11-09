package edu.xtu.bbs.user.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "bbs.oss")
@Component
public class OssConfiguration {
    private String secretId;

    private String secretKey;

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
