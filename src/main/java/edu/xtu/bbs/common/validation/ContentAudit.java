package edu.xtu.bbs.common.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ContentAuditValidator.class)
public @interface ContentAudit {

    String message() default "Content contains prohibited expressions.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
