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

package com.vip.saturn.job.console.domain;

import java.io.Serializable;

public final class ExecutionInfo implements Serializable, Comparable<ExecutionInfo> {

	private static final long serialVersionUID = 8587397581949456718L;

	private String jobName;

	private String timeZone;

	private int item;

	private ExecutionStatus status;

	private String jobMsg;

	private boolean failover;

	private String lastBeginTime;

	private String nextFireTime;

	private String lastCompleteTime;
	/**
	 * 作业运行时间，与zk lastCompleteTime比较 需保证zk与console时间同步
	 */
	private long timeConsumed;

	// 上次执行完成的耗时
	private double lastTimeConsumedInSec;

	/** 运行作业服务器 */
	private String executorName;

	/** 作业分片运行日志 */
	private String logMsg;

	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public int getItem() {
		return item;
	}

	public void setItem(int item) {
		this.item = item;
	}

	public ExecutionStatus getStatus() {
		return status;
	}

	public void setStatus(ExecutionStatus status) {
		this.status = status;
	}

	public String getJobMsg() {
		return jobMsg;
	}

	public void setJobMsg(String jobMsg) {
		this.jobMsg = jobMsg;
	}

	public boolean getFailover() {
		return failover;
	}

	public void setFailover(boolean failover) {
		this.failover = failover;
	}

	public String getLastBeginTime() {
		return lastBeginTime;
	}

	public void setLastBeginTime(String lastBeginTime) {
		this.lastBeginTime = lastBeginTime;
	}

	public String getNextFireTime() {
		return nextFireTime;
	}

	public void setNextFireTime(String nextFireTime) {
		this.nextFireTime = nextFireTime;
	}

	public String getLastCompleteTime() {
		return lastCompleteTime;
	}

	public void setLastCompleteTime(String lastCompleteTime) {
		this.lastCompleteTime = lastCompleteTime;
	}

	public long getTimeConsumed() {
		return timeConsumed;
	}

	public void setTimeConsumed(long timeConsumed) {
		this.timeConsumed = timeConsumed;
	}

	public String getExecutorName() {
		return executorName;
	}

	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}

	public String getLogMsg() {
		return logMsg;
	}

	public void setLogMsg(String logMsg) {
		this.logMsg = logMsg;
	}

	public double getLastTimeConsumedInSec() {
		return lastTimeConsumedInSec;
	}

	public void setLastTimeConsumedInSec(double lastTimeConsumedInSec) {
		this.lastTimeConsumedInSec = lastTimeConsumedInSec;
	}

	@Override
	public int compareTo(final ExecutionInfo o) {
		return getItem() - o.getItem();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + item;
		result = prime * result + ((jobName == null) ? 0 : jobName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ExecutionInfo other = (ExecutionInfo) obj;
		if (item != other.item) {
			return false;
		}
		if (jobName == null) {
			if (other.jobName != null) {
				return false;
			}
		} else if (!jobName.equals(other.jobName)) {
			return false;
		}
		return true;
	}

	public enum ExecutionStatus {
		RUNNING, COMPLETED,
		// 执行失败
		FAILED,
		// 执行超时
		TIMEOUT, PENDING, BLANK;

		public static ExecutionStatus getExecutionStatus(final boolean running, final boolean completed, boolean failed,
				boolean timeout, boolean isEnabledReport) {
			if (!isEnabledReport) {
				return ExecutionStatus.BLANK;
			}
			if (running) {
				return ExecutionStatus.RUNNING;
			}
			if (completed) {
				if (failed) {
					return ExecutionStatus.FAILED;
				} else if (timeout) {
					return ExecutionStatus.TIMEOUT;
				} else {
					return ExecutionStatus.COMPLETED;
				}
			}
			return ExecutionStatus.PENDING;
		}
	}
}
