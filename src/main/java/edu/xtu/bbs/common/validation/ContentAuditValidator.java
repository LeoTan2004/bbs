package edu.xtu.bbs.common.validation;

import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentAuditValidator implements ConstraintValidator<ContentAudit, String> {

    private final SensitiveWordsDetector detector;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            return !detector.containsSensitiveWords(value);
        } catch (RuntimeException ex) {
            log.error("Failed to audit content against sensitive words.", ex);
            return true;
        }
    }
}
