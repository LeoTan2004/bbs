package edu.xtu.bbs.user.exception;

import lombok.Getter;

@Getter
public class WeChatAppUserNotFoundException extends Exception {
    private final String openId;

    public WeChatAppUserNotFoundException(String openId, String msg) {
        super(msg);
        this.openId = openId;
    }

}
