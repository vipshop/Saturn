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

package com.vip.saturn.job.basic;

import java.util.Map;

/**
 * 作业运行时分片上下文抽象类.
 * @author dylan.xue
 */
public abstract class AbstractJobExecutionShardingContext {

	/**
	 * 作业名称.
	 */
	private String jobName;

	/**
	 * 分片总数.
	 */
	private int shardingTotalCount;

	/**
	 * 作业自定义参数. 可以配置多个相同的作业, 但是用不同的参数作为不同的调度实例.
	 */
	private String jobParameter;

	/**
	 * 自定义上下文
	 */
	private Map<String, String> customContext;

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public int getShardingTotalCount() {
		return shardingTotalCount;
	}

	public void setShardingTotalCount(int shardingTotalCount) {
		this.shardingTotalCount = shardingTotalCount;
	}

	public String getJobParameter() {
		return jobParameter;
	}

	public void setJobParameter(String jobParameter) {
		this.jobParameter = jobParameter;
	}

	public Map<String, String> getCustomContext() {
		return customContext;
	}

	public void setCustomContext(Map<String, String> customContext) {
		this.customContext = customContext;
	}

}
