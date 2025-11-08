package edu.xtu.bbs.user.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UploadRequest(
        @NotNull String type,
        @NotNull @Size(min = 1) Long size
) {
}
