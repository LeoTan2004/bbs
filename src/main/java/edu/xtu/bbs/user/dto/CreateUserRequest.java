package edu.xtu.bbs.user.dto;

import edu.xtu.bbs.common.validation.ContentAudit;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @ContentAudit @NotBlank @Size(min = 2, max = 50) String username,
        @NotBlank @Size(min = 8, max = 20) String password,
        @Email @NotBlank String email,
        @ContentAudit @Size(max = 100) String nickname,
        @Size(max = 1023) String bio
) {
}