package edu.xtu.bbs.report.dto;

import edu.xtu.bbs.report.model.ReportStatus;

import java.io.Serializable;
import java.time.Instant;

/**
 * Response payload representing a user report.
 */
public record ReportResponse(
        Long id,
        ReportStatus status,
        ReportTargetInfo target,
        String reason,
        String description,
        String result,
        Boolean resultNotified,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}
