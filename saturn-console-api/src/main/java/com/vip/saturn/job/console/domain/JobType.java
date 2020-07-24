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

package com.vip.saturn.job.console.domain;

/**
 * @author hebelala
 */
public enum JobType {

	JAVA_JOB, SHELL_JOB, PASSIVE_JAVA_JOB, PASSIVE_SHELL_JOB, MSG_JOB, VSHELL, UNKNOWN_JOB;

	public static final JobType getJobType(String jobType) {
		try {
			return valueOf(jobType);
		} catch (Exception e) {
			return UNKNOWN_JOB;
		}
	}

	public static boolean isCron(JobType jobType) {
		return JAVA_JOB == jobType || SHELL_JOB == jobType;
	}

	public static boolean isPassive(JobType jobType) {
		return PASSIVE_JAVA_JOB == jobType || PASSIVE_SHELL_JOB == jobType;
	}

	public static boolean isJava(JobType jobType) {
		return JAVA_JOB == jobType || PASSIVE_JAVA_JOB == jobType || MSG_JOB == jobType;
	}

	public static boolean isShell(JobType jobType) {
		return SHELL_JOB == jobType || PASSIVE_SHELL_JOB == jobType || VSHELL == jobType;
	}

	public static boolean isMsg(JobType jobType) {
		return MSG_JOB == jobType || VSHELL == jobType;
	}

}
