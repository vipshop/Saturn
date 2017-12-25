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
	
	private boolean noTraffic;
	
	public ServerBriefInfo(String executorName) {
		this.executorName = executorName;
	}

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

	public boolean isNoTraffic() {
		return noTraffic;
	}

	public void setNoTraffic(boolean noTraffic) {
		this.noTraffic = noTraffic;
	}

}
