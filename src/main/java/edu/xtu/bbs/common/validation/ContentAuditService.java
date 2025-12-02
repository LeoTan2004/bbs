package edu.xtu.bbs.common.validation;

import edu.xtu.bbs.common.exception.ContentAuditViolationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Utility component to trigger {@link ContentAudit} constraints programmatically.
 */
@Component
public class ContentAuditService {

    private final Validator validator;

    public ContentAuditService(Validator validator) {
        this.validator = validator;
    }

    /**
     * Validates the provided target object and throws {@link ContentAuditViolationException}
     * if any {@link ContentAudit}-annotated property fails validation.
     *
     * @param target object that may contain {@link ContentAudit} annotations
     */
    public void validate(Object target) {
        if (target == null) {
            return;
        }

        Set<ConstraintViolation<Object>> violations = validator.validate(target);
        for (ConstraintViolation<Object> violation : violations) {
            Annotation annotation = violation.getConstraintDescriptor().getAnnotation();
            if (annotation != null && annotation.annotationType() == ContentAudit.class) {
                throw new ContentAuditViolationException(
                        violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : null,
                        violation.getMessage());
            }
        }
    }
}
