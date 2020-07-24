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

import com.google.common.collect.Maps;
import java.util.Map;

public class ServerAllocationInfo {

	private String executorName;

	private int totalLoadLevel;

	// key为jobName，value是分片item号列表
	private Map<String, String> allocationMap = Maps.newHashMap();

	public ServerAllocationInfo(String executorName) {
		this.executorName = executorName;
	}

	public int getTotalLoadLevel() {
		return totalLoadLevel;
	}

	public void setTotalLoadLevel(int totalLoadLevel) {
		this.totalLoadLevel = totalLoadLevel;
	}

	public Map<String, String> getAllocationMap() {
		return allocationMap;
	}

	public void setAllocationMap(Map<String, String> allocationMap) {
		this.allocationMap = allocationMap;
	}

	public String getExecutorName() {
		return executorName;
	}

	public void setExecutorName(String executorName) {
		this.executorName = executorName;
	}
}
