package edu.xtu.bbs.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "oss.avatar")
@Component
@Data
public class AvatarConfiguration {

    private String bucket;

    private String prefix;

    private DataSize maxSize;

    private List<String> allowTypes;

    private Duration expiredAfter;
}
