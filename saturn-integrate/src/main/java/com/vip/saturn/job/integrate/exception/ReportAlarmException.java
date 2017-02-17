package com.vip.saturn.job.integrate.exception;

/**
 * @author hebelala
 */
public class ReportAlarmException extends Exception {

    public ReportAlarmException() {
    }

    public ReportAlarmException(String message) {
        super(message);
    }

    public ReportAlarmException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReportAlarmException(Throwable cause) {
        super(cause);
    }

    public ReportAlarmException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
