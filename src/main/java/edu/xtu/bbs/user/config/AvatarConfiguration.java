package edu.xtu.bbs.user.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "bbs.oss.avatar")
@Component
@Data
public class AvatarConfiguration {

    @NotBlank
    private String bucket;

    private String prefix = "/avatars";

    private DataSize maxSize = DataSize.ofMegabytes(5);

    private List<String> allowTypes = List.of("image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp");

    private Duration expiredAfter = Duration.ofSeconds(30);
}
