/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/
package com.vip.saturn.job.console.exception;

/**
 * @author yangjuanying
 */
public class SaturnJobConsoleException extends Exception {

	public static final int ERROR_CODE_NOT_EXISTED = 1;

	public static final int ERROR_CODE_BAD_REQUEST = 2;

	public static final int ERROR_CODE_INTERNAL_ERROR = 0;

	public static final int ERROR_CODE_AUTHN_FAIL = 4;

	private int errorCode = ERROR_CODE_INTERNAL_ERROR;

	public SaturnJobConsoleException() {
	}

	public SaturnJobConsoleException(String message) {
		super(message);
		this.errorCode = ERROR_CODE_INTERNAL_ERROR;
	}

	public SaturnJobConsoleException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public SaturnJobConsoleException(String message, Throwable cause) {
		super(message, cause);
		this.errorCode = ERROR_CODE_INTERNAL_ERROR;
	}

	public SaturnJobConsoleException(int errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	public SaturnJobConsoleException(Throwable cause) {
		super(cause);
	}

	public SaturnJobConsoleException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public int getErrorCode() {
		return errorCode;
	}
}
