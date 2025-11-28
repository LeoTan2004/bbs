package edu.xtu.bbs.post.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
public class Medium implements Serializable {
    @Serial
    private static final long serialVersionUID = 13216348643215464L;

    private String id;
    private String displayUrl;
    private String resourceUrl;
    private String type;
}
