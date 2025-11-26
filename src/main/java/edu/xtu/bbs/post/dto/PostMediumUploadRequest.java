package edu.xtu.bbs.post.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostMediumUploadRequest(@NotNull String type,
                                      @NotNull @Size(min = 1) Long size) {

}
