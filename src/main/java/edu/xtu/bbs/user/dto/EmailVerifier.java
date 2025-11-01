package edu.xtu.bbs.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerifier(
        @NotBlank(message = "Email must not be blank") @Email(message = "Invalid email format") String email,
        @NotBlank(message = "token must not be blank") String token,
        @NotBlank(message = "Verification code must not be blank") String code
) {
}
