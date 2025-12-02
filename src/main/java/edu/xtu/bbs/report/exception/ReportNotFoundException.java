package edu.xtu.bbs.report.exception;

import edu.xtu.bbs.common.exception.BusinessException;
import edu.xtu.bbs.common.response.ResponseCode;

/**
 * Thrown when the requested report does not exist or is inaccessible.
 */
public class ReportNotFoundException extends BusinessException {

    public ReportNotFoundException(Long reportId) {
        super(ResponseCode.REPORT_NOT_FOUND, "Report not found: " + reportId);
    }
}
