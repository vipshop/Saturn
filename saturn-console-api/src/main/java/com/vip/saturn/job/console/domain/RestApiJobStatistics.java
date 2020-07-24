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
 * @author xiaopeng.he
 */
public class RestApiJobStatistics {

	private Long lastBeginTime;

	private Long lastCompleteTime;

	private Long nextFireTime;

	private Long processCount;

	private Long processErrorCount;

	public Long getLastBeginTime() {
		return lastBeginTime;
	}

	public void setLastBeginTime(Long lastBeginTime) {
		this.lastBeginTime = lastBeginTime;
	}

	public Long getLastCompleteTime() {
		return lastCompleteTime;
	}

	public void setLastCompleteTime(Long lastCompleteTime) {
		this.lastCompleteTime = lastCompleteTime;
	}

	public Long getNextFireTime() {
		return nextFireTime;
	}

	public void setNextFireTime(Long nextFireTime) {
		this.nextFireTime = nextFireTime;
	}

	public Long getProcessCount() {
		return processCount;
	}

	public void setProcessCount(Long processCount) {
		this.processCount = processCount;
	}

	public Long getProcessErrorCount() {
		return processErrorCount;
	}

	public void setProcessErrorCount(Long processErrorCount) {
		this.processErrorCount = processErrorCount;
	}
}
