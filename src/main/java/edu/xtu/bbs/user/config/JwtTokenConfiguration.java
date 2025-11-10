package edu.xtu.bbs.user.config;

import io.jsonwebtoken.io.Encoders;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@ConfigurationProperties(prefix = "bbs.security.jwt")
@Component
@Data
public class JwtTokenConfiguration {
    /**
     * Base64 URL-safe encoded secret key for signing JWT tokens
     * If not set, a random one will be generated at startup
     */
    private String secretKey;

    private Duration expiredTime = Duration.ofHours(2);

    public String getSecretKey() {
        if (secretKey == null || secretKey.isBlank()) {
            final String s = JwtSecretUtils.generateBase64Secret();
            log.warn("JWT secret key is not set! Generated a random one: {}", s);
            secretKey = s;
        }
        return secretKey;
    }

    private static final class JwtSecretUtils {
        private static final int DEFAULT_BYTE_LEN = 64;   // 512 bit
        private static final SecureRandom RNG = new SecureRandom();

        private JwtSecretUtils() {
        }

        /**
         * randomly generate a base64 url-safe secret string
         */
        public static String generateBase64Secret() {
            return generateBase64Secret(DEFAULT_BYTE_LEN);
        }

        public static String generateBase64Secret(int byteLen) {
            byte[] bytes = new byte[byteLen];
            RNG.nextBytes(bytes);
            return Encoders.BASE64.encode(bytes);
        }
    }
}
