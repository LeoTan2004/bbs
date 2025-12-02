package edu.xtu.bbs.common.validation;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "bbs.validator.sensitive-words")
public class SensitiveWordProps {

    /**
     * File system path or remote URL that contains the original sensitive words list.
     */
    private Resource file;

    /**
     * List of sensitive words to include in addition to those loaded from file.
     */
    private Set<String> include = new LinkedHashSet<>();
}
