package edu.xtu.bbs.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@ConfigurationProperties(prefix = "security.jwt")
@Component
@Data
public class JwtTokenConfiguration {
    private String secretKey;

    private Duration expiredTime = Duration.ofHours(2);
}
