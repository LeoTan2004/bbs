package edu.xtu.bbs.verification;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for {@link VerificationRequest}
 *
 * @param principle
 * @param token
 * @param credential
 */
public record VerificationParam(
        @NotBlank(message = "principle must not be blank") String principle,
        @NotBlank(message = "token must not be blank") String token,
        @NotBlank(message = "scope must not be blank") String scope,
        @NotBlank(message = "credential must not be blank") String credential
) {
}
