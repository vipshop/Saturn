/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.exception;

/**
 * 调用了shutdown， 但作业还在调度时抛该异常。
 * 
 * @author chembo.huang
 */
public final class JobShuttingDownException extends Exception {

	private static final long serialVersionUID = -6287464997081326084L;

	private static final String ERROR_MSG = "Job is shutting down, job shouldn't be invoked.";

	public JobShuttingDownException() {
		super(ERROR_MSG);
	}
}
