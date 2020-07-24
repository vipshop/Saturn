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

package com.vip.saturn.job.exception;

/**
 * 
 * @author xiaopeng.he
 *
 */

public class SaturnJobException extends Exception {

	private static final long serialVersionUID = 1L;

	public static final int ILLEGAL_ARGUMENT = 0;

	public static final int JOB_NOT_FOUND = 1;

	public static final int OUT_OF_ZK_LIMIT_MEMORY = 3;

	public static final int JOB_NAME_INVALID = 4;

	public static final int SYSTEM_ERROR = 5;

	private int type;

	private String message;

	public SaturnJobException(String message) {
		this(SYSTEM_ERROR, message);
	}

	public SaturnJobException(int type, String message) {
		super();
		this.type = type;
		this.message = message;
	}

	public SaturnJobException(int type, String message, Throwable cause) {
		super(cause);
		this.type = type;
		this.message = message;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	@Override
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
