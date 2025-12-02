package edu.xtu.bbs.report.exception;

import edu.xtu.bbs.common.exception.BusinessException;
import edu.xtu.bbs.common.response.ResponseCode;

/**
 * Thrown when attempting to process a report that is no longer pending.
 */
public class ReportAlreadyProcessedException extends BusinessException {

    public ReportAlreadyProcessedException(Long reportId) {
        super(ResponseCode.REPORT_ALREADY_RESOLVED,
                "Report has already been processed: " + reportId);
    }
}
