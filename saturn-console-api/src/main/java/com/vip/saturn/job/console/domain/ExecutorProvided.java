/**
 * Copyright 1999-2015 dangdang.com.
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

public class ExecutorProvided {

	private String executorName;
	private ExecutorProvidedType type;
	private ExecutorProvidedStatus status;
	private Boolean noTraffic;
	private String ip;

	public String getExecutorName() {
		return executorName;
	}

	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}

	public ExecutorProvidedType getType() {
		return type;
	}

	public void setType(ExecutorProvidedType type) {
		this.type = type;
	}

	public ExecutorProvidedStatus getStatus() {
		return status;
	}

	public void setStatus(ExecutorProvidedStatus status) {
		this.status = status;
	}

	public Boolean isNoTraffic() {
		return noTraffic;
	}

	public void setNoTraffic(Boolean noTraffic) {
		this.noTraffic = noTraffic;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}
}
