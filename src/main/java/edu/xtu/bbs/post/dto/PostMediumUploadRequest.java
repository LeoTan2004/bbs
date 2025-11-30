package edu.xtu.bbs.post.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PostMediumUploadRequest(@NotNull String type,
                                      @NotNull @Min(1) Long size) {

}
