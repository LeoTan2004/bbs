package edu.xtu.bbs.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * WeChat Registration Request DTO
 * Used for registering users with WeChat
 */
public record WeChatRegisterRequest(
        @NotBlank(message = "WeChat code must not be blank") String code,
        @NotBlank @Size(min = 2, max = 50) String username,
        @NotBlank @Size(min = 8, max = 20) String password,
        @Size(max = 100) String nickname,
        @Size(max = 1023) String bio
) {
}