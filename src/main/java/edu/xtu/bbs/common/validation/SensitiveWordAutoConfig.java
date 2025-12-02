package edu.xtu.bbs.common.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableConfigurationProperties(SensitiveWordProps.class)
@Slf4j
public class SensitiveWordAutoConfig {

    @Bean
    public SensitiveWordsDetector sensitiveWordsDetector(SensitiveWordProps props) throws IOException {
        final Resource resource = props.getFile();
        final Set<String> words = new LinkedHashSet<>(props.getInclude());

        if (resource != null) {
            loadWordsFromResource(resource, words);
        }

        if (words.isEmpty()) {
            log.warn("No sensitive words configured; ContentAudit will not block any content.");
        } else {
            log.info("Loaded {} sensitive words via configuration properties.", words.size());
        }

        final SensitiveWordsDetector detector = new SensitiveWordsDetector();
        detector.setSensitiveWords(words);
        return detector;
    }

    private void loadWordsFromResource(Resource resource, Set<String> words) {
        try {
            if (!resource.exists()) {
                log.warn("Sensitive words resource not found at {}", resource.getDescription());
                return;
            }

            log.debug("Loading sensitive words from {}", resource.getDescription());

            try (InputStream inputStream = resource.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .forEach(words::add);
            }
        } catch (IOException ex) {
            log.error("Failed to load sensitive words from {}", resource.getDescription(), ex);
        }
    }
}
