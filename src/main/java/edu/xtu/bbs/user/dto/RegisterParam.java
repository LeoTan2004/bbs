package edu.xtu.bbs.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for {@link edu.xtu.bbs.user.model.User}
 *
 * @param username
 * @param password
 * @param email
 * @param code
 */
public record RegisterParam(
        @NotBlank @Size(min = 2, max = 100) String username,
        @NotBlank @Size(min = 8, max = 100) String password,
        @Email(message = "Email is required") String email,
        @NotBlank @Size(min = 6, max = 6) String code
) {

}
