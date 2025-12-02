package edu.xtu.bbs.report.dto;

import edu.xtu.bbs.common.validation.ContentAudit;
import edu.xtu.bbs.report.model.ReportTargetType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * Request payload for creating a report.
 */
public record CreateReportRequest(
        @NotNull ReportTargetType targetType,
        @NotNull @Min(1) Integer targetId,
        @ContentAudit @NotBlank @Size(max = 255) String reason,
        @ContentAudit @Size(max = 2000) String description
) implements Serializable {
}
