package edu.xtu.bbs.user.exception;

import lombok.Getter;

/**
 * Exception thrown when trying to register with a WeChat OpenID that already exists
 */
@Getter
public class WeChatOpenIdAlreadyExistsException extends Exception {
    private final String openId;

    public WeChatOpenIdAlreadyExistsException(String openId) {
        super("WeChat OpenID already exists: " + openId);
        this.openId = openId;
    }

    @Override
    public String toString() {
        return "WeChatOpenIdAlreadyExistsException: " + openId + " already exists";
    }
}