/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.vip.saturn.job.console.domain;

import java.io.Serializable;

/**
 * @author chembo.huang
 *
 */
public final class ServerBriefInfo implements Serializable {
    
    private static final long serialVersionUID = 1133149706443681483L;
    
    private String serverIp;
    
    private final String executorName;
    
    private Integer totalLoadLevel;
    
    private String sharding;
    
    private Boolean hasSharding;
    
    private String lastBeginTime;
    
    private ServerStatus status;
    
    private String version;
    
	public String getServerIp() {
		return this.serverIp;
	}

	public String getExecutorName() {
		return this.executorName;
	}

	public Integer getTotalLoadLevel() {
		return this.totalLoadLevel;
	}

	public String getSharding() {
		return this.sharding;
	}

	public Boolean getHasSharding() {
		return this.hasSharding;
	}

	public String getLastBeginTime() {
		return this.lastBeginTime;
	}

	public ServerStatus getStatus() {
		return this.status;
	}

	public String getVersion() {
		return this.version;
	}

	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}

	public void setTotalLoadLevel(Integer totalLoadLevel) {
		this.totalLoadLevel = totalLoadLevel;
	}

	public void setSharding(String sharding) {
		this.sharding = sharding;
	}

	public void setHasSharding(Boolean hasSharding) {
		this.hasSharding = hasSharding;
	}

	public void setLastBeginTime(String lastBeginTime) {
		this.lastBeginTime = lastBeginTime;
	}

	public void setStatus(ServerStatus status) {
		this.status = status;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof ServerBriefInfo))
			return false;
		ServerBriefInfo other = (ServerBriefInfo) o;
		Object this$executorName = getExecutorName();
		Object other$executorName = other.getExecutorName();
		if (this$executorName == null ? other$executorName != null : !this$executorName.equals(other$executorName))
			return false;
		Object this$totalLoadLevel = getTotalLoadLevel();
		Object other$totalLoadLevel = other.getTotalLoadLevel();
		if (this$totalLoadLevel == null ? other$totalLoadLevel != null
				: !this$totalLoadLevel.equals(other$totalLoadLevel))
			return false;
		Object this$sharding = getSharding();
		Object other$sharding = other.getSharding();
		if (this$sharding == null ? other$sharding != null : !this$sharding.equals(other$sharding))
			return false;
		Object this$hasSharding = getHasSharding();
		Object other$hasSharding = other.getHasSharding();
		if (this$hasSharding == null ? other$hasSharding != null : !this$hasSharding.equals(other$hasSharding))
			return false;
		Object this$lastBeginTime = getLastBeginTime();
		Object other$lastBeginTime = other.getLastBeginTime();
		if (this$lastBeginTime == null ? other$lastBeginTime != null : !this$lastBeginTime.equals(other$lastBeginTime))
			return false;
		Object this$status = getStatus();
		Object other$status = other.getStatus();
		if (this$status == null ? other$status != null : !this$status.equals(other$status))
			return false;
		Object this$version = getVersion();
		Object other$version = other.getVersion();
		return this$version == null ? other$version == null : this$version.equals(other$version);
	}

	public int hashCode() {
		int PRIME = 59;
		int result = 1;
		Object $executorName = getExecutorName();
		result = result * 59 + ($executorName == null ? 43 : $executorName.hashCode());
		Object $totalLoadLevel = getTotalLoadLevel();
		result = result * 59 + ($totalLoadLevel == null ? 43 : $totalLoadLevel.hashCode());
		Object $sharding = getSharding();
		result = result * 59 + ($sharding == null ? 43 : $sharding.hashCode());
		Object $hasSharding = getHasSharding();
		result = result * 59 + ($hasSharding == null ? 43 : $hasSharding.hashCode());
		Object $lastBeginTime = getLastBeginTime();
		result = result * 59 + ($lastBeginTime == null ? 43 : $lastBeginTime.hashCode());
		Object $status = getStatus();
		result = result * 59 + ($status == null ? 43 : $status.hashCode());
		Object $version = getVersion();
		return result * 59 + ($version == null ? 43 : $version.hashCode());
	}

	public ServerBriefInfo(String executorName) {
		this.executorName = executorName;
	}
}
