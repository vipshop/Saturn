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

/**
 * 作业运行时单片分片上下文.
 * @author dylan.xue
 */
public final class JobExecutionSingleShardingContext extends AbstractJobExecutionShardingContext {

	/**
	 * 运行在本作业服务器的分片序列号.
	 */
	private int shardingItem;

	/**
	 * 运行在本作业项的分片序列号和个性化参数.
	 */
	private String shardingItemParameter;

	/**
	 * 数据处理位置.
	 */
	private String offset;

	public int getShardingItem() {
		return shardingItem;
	}

	public void setShardingItem(int shardingItem) {
		this.shardingItem = shardingItem;
	}

	public String getShardingItemParameter() {
		return shardingItemParameter;
	}

	public void setShardingItemParameter(String shardingItemParameter) {
		this.shardingItemParameter = shardingItemParameter;
	}

	public String getOffset() {
		return offset;
	}

	public void setOffset(String offset) {
		this.offset = offset;
	}

}
