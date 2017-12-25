package com.vip.saturn.job.console.exception;

/**
 * GUI Exception.
 */
public class SaturnJobConsoleGUIException extends SaturnJobConsoleException{

	public SaturnJobConsoleGUIException() {
	}

	public SaturnJobConsoleGUIException(String message) {
		super(message);
	}

	public SaturnJobConsoleGUIException(String message, Throwable cause) {
		super(message, cause);
	}

	public SaturnJobConsoleGUIException(Throwable cause) {
		super(cause);
	}

	public SaturnJobConsoleGUIException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
