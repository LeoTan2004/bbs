package edu.xtu.bbs.user.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for updating user profile
 */
public record UpdateProfileRequest(
        @Size(max = 100) String nickname,
        @Size(max = 1023) String bio,
        @Size(max = 100) String profileSlug
) {
}