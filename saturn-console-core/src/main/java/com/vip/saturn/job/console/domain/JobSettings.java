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

package com.vip.saturn.job.console.domain;

/**
 * @author chembo.huang
 *
 */
public final class JobSettings extends JobConfig {

	private static final long serialVersionUID = 6093075475155512194L;

	public String updateFields() {
		StringBuilder sb = new StringBuilder(100);
		sb.append("{").append("jobName=").append(getJobName()).append(", ").append("shardingTotalCount=")
				.append(getShardingTotalCount()).append(", ").append("loadLevel=").append(getLoadLevel()).append(", ")
				.append("jobDegree=").append(getJobDegree()).append(", ").append("enabledReport=")
				.append(getEnabledReport()).append(", ").append("timeZone=").append(getTimeZone()).append(", ")
				.append("cron=").append(getCron()).append(", ").append("pausePeriodDate=").append(getPausePeriodDate())
				.append(", ").append("pausePeriodTime=").append(getPausePeriodTime()).append(", ")
				.append("shardingItemParameters=").append(getShardingItemParameters()).append(", ")
				.append("jobParameter=").append(getJobParameter()).append(", ").append("processCountIntervalSeconds=")
				.append(getProcessCountIntervalSeconds()).append(", ").append("timeout4AlarmSeconds=")
				.append(getTimeout4AlarmSeconds()).append(", ").append("timeoutSeconds=").append(getTimeoutSeconds())
				.append(", ").append("failover=").append(getFailover()).append(", ").append("dependencies=")
				.append(getDependencies()).append(", ").append("groups=").append(getGroups()).append(", ")
				.append("description=").append(getDescription()).append(", ").append("channelName=")
				.append(getChannelName()).append(", ").append("queueName=").append(getQueueName()).append(", ")
				.append("preferList=").append(getPreferList()).append(", ").append("useDispreferList=")
				.append(getUseDispreferList()).append(", ").append("localMode=").append(getLocalMode()).append(", ")
				.append("showNormalLog=").append(getShowNormalLog()).append(", ").append("useSerial=")
				.append(getUseSerial()).append("}");
		return sb.toString();
	}

}
