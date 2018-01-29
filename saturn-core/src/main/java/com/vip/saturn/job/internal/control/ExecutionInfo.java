/**
 * Copyright 2016 vip.com. <p> Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. </p>
 */

package com.vip.saturn.job.internal.control;

import java.io.Serializable;

public final class ExecutionInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private int item;

	private String jobMsg;

	private Long lastBeginTime;

	private Long lastCompleteTime;

	private Long nextFireTime;

	/** 作业分片运行日志 */
	private String jobLog;

	public ExecutionInfo() {
	}

	public ExecutionInfo(int item) {
		this.item = item;
	}

	public ExecutionInfo(int item, Long lastBeginTime) {
		this.item = item;
		this.lastBeginTime = lastBeginTime;
	}

	public String getJobLog() {
		return jobLog;
	}

	public void setJobLog(String jobLog) {
		this.jobLog = jobLog;
	}

	public int getItem() {
		return item;
	}

	public void setItem(int item) {
		this.item = item;
	}

	public String getJobMsg() {
		return jobMsg;
	}

	public void setJobMsg(String jobMsg) {
		this.jobMsg = jobMsg;
	}

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

	@Override
	public String toString() {
		return "ExecutionInfo [item=" + item + ", jobMsg=" + jobMsg + ", lastBeginTime=" + lastBeginTime
				+ ", lastCompleteTime=" + lastCompleteTime + ", nextFireTime=" + nextFireTime + ", jobLog=" + jobLog
				+ "]";
	}

}
