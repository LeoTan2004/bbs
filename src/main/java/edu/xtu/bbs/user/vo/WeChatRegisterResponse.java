package edu.xtu.bbs.user.vo;

/**
 * WeChat Registration Response VO
 * Used for returning WeChat registration result
 */
public record WeChatRegisterResponse(String openId, String message) {

    public WeChatRegisterResponse(String openId) {
        this(openId, "WeChat registration successful");
    }
}