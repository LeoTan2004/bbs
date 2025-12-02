package edu.xtu.bbs.report.dto;

import edu.xtu.bbs.common.validation.ContentAudit;
import edu.xtu.bbs.report.model.ReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * Request payload for publishing report handling result.
 */
public record ReportResultRequest(
        @NotNull ReportStatus status,
        @ContentAudit @NotBlank @Size(max = 2000) String result,
        @Size(max = 1024) String redirectUrl
) implements Serializable {
}
