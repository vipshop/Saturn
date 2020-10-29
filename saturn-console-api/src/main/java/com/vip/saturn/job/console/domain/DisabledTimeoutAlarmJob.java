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
 * 禁用作业超时告警对象
 */
public class DisabledTimeoutAlarmJob extends AbstractAlarmJob {

	/**
	 * 禁用作业的时间
	 */
	private long disableTime;

	/**
	 * 字符串格式的禁用作业时间
	 */
	private String disableTimeStr;

	/**
	 * 设置的禁用超时秒数
	 */
	private int disableTimeoutSeconds;

	public DisabledTimeoutAlarmJob() {
	}

	public DisabledTimeoutAlarmJob(String jobName, String domainName, String nns, String degree) {
		super(jobName, domainName, nns, degree);
	}

	public long getDisableTime() {
		return disableTime;
	}

	public void setDisableTime(long disableTime) {
		this.disableTime = disableTime;
	}

	public String getDisableTimeStr() {
		return disableTimeStr;
	}

	public void setDisableTimeStr(String disableTimeStr) {
		this.disableTimeStr = disableTimeStr;
	}

	public int getDisableTimeoutSeconds() {
		return disableTimeoutSeconds;
	}

	public void setDisableTimeoutSeconds(int disableTimeoutSeconds) {
		this.disableTimeoutSeconds = disableTimeoutSeconds;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		DisabledTimeoutAlarmJob that = (DisabledTimeoutAlarmJob) o;

		if (!jobName.equals(that.jobName)) {
			return false;
		}
		return domainName.equals(that.domainName);
	}

	@Override
	public int hashCode() {
		return (int) (disableTime ^ (disableTime >>> 32));
	}
}
