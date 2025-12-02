package edu.xtu.bbs.report.exception;

import edu.xtu.bbs.common.exception.BusinessException;
import edu.xtu.bbs.common.response.ResponseCode;
import edu.xtu.bbs.report.model.ReportTargetType;

/**
 * Thrown when the reported target cannot be found.
 */
public class InvalidReportTargetException extends BusinessException {

    public InvalidReportTargetException(ReportTargetType type, Integer targetId) {
        super(ResponseCode.REPORT_TARGET_NOT_FOUND,
                "Reported target not found: " + type + "=" + targetId);
    }
}
