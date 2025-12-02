package edu.xtu.bbs.report.dto;

import edu.xtu.bbs.report.model.ReportTargetType;

import java.io.Serializable;

/**
 * Lightweight summary of the reported target.
 */
public record ReportTargetInfo(
        ReportTargetType type,
        Integer id,
        String title,
        String snippet
) implements Serializable {
}
