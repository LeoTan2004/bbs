package edu.xtu.bbs.user.dto;

/**
 * WeChat App Session DTO
 * <p>
 * Response from WeChat API for jscode2session
 * </p>
 *
 * @param session_key Session key
 * @param unionid     Union ID, unique identifier for user across multiple apps
 * @param errcode     Error Code
 * @param errmsg      Error Message
 * @param openid      Open ID, unique identifier for user in the current app
 */
public record WeChatAppSessionDto(
        String session_key,
        String unionid,
        Integer errcode,
        String errmsg,
        String openid
) {
}
