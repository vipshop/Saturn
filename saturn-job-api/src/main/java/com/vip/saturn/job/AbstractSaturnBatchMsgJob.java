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

package com.vip.saturn.job;

import com.vip.saturn.job.BaseSaturnJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;
import com.vip.saturn.job.msg.MsgHolder;

import java.util.List;

public abstract class AbstractSaturnBatchMsgJob extends BaseSaturnJob {

	/**
	 * 消息作业处理入口
	 * @param jobName 作业名
	 * @param shardItem 分片项
	 * @param shardParam 分片参数
	 * @param msgHolder 消息内容
	 * @param shardingContext 其它参数信息
	 * @return 返回执行结果
	 * @throws InterruptedException 注意处理中断异常
	 */
	public abstract SaturnJobReturn handleMsgJob(String jobName, Integer shardItem, String shardParam,
			MsgHolder msgHolder, SaturnJobExecutionContext shardingContext) throws InterruptedException;

	/**
	 * 超时强杀之前调用此方法
	 * @param jobName 作业名
	 * @param shardItem 分片项
	 * @param shardParam 分片参数
	 * @param msgHolder 消息内容
	 * @param shardingContext 其它参数信息
	 */
	public void beforeTimeout(String jobName, Integer shardItem, String shardParam, MsgHolder msgHolder,
			SaturnJobExecutionContext shardingContext) {
		// 由作业类实现逻辑
	}

	/**
	 * 超时强杀之后调用此方法
	 * @param jobName 作业名
	 * @param shardItem 分片项
	 * @param shardParam 分片参数
	 * @param msgHolder 消息内容
	 * @param shardingContext 其它参数信息
	 */
	public void onTimeout(String jobName, Integer shardItem, String shardParam, MsgHolder msgHolder,
			SaturnJobExecutionContext shardingContext) {
		// 由作业类实现逻辑
	}

	/**
	 * 在saturn-console对作业立即终止，或者优雅退出超时，或者与zk失去连接时，都会在强杀业务线程之前调用此方法。
	 * <p>
	 * 注意，作业执行超时，强杀之前不会调用此方法，而是调用{@link #beforeTimeout(String, Integer, String, MsgHolder, SaturnJobExecutionContext)}方法。
	 * @param jobName 作业名
	 * @param shardItem 分片项
	 * @param shardParam 分片参数
	 * @param msgHolder 消息内容
	 * @param shardingContext 其它参数信息
	 */
	public void beforeForceStop(String jobName, Integer shardItem, String shardParam, MsgHolder msgHolder,
			SaturnJobExecutionContext shardingContext) {
		// 由作业类实现逻辑
	}

	/**
	 * 在saturn-console对作业立即终止，或者优雅退出超时，或者与zk失去连接时，都会在强杀业务线程之后调用此方法。
	 * <p>
	 * 注意，作业执行超时，强杀之后不会调用此方法，而是调用{@link #onTimeout(String, Integer, String, MsgHolder, SaturnJobExecutionContext)}方法。
	 * @param jobName 作业名
	 * @param shardItem 分片项
	 * @param shardParam 分片参数
	 * @param msgHolder 消息内容
	 * @param shardingContext 其它参数信息
	 */
	public void postForceStop(String jobName, Integer shardItem, String shardParam, MsgHolder msgHolder,
			SaturnJobExecutionContext shardingContext) {
		// 由作业类实现逻辑
	}

	/**
	 * 批量消息作业处理入口
	 * @param jobName 作业名
	 * @param shardItem 分片项
	 * @param shardParam 分片参数
	 * @param msgHolders 消息内容
	 * @param shardingContext 其它参数信息
	 * @return 返回执行结果
	 * @throws InterruptedException 注意处理中断异常
	 */
	public abstract SaturnJobReturn handleBatchMsgJob(String jobName, Integer shardItem, String shardParam,
			List<MsgHolder> msgHolders, SaturnJobExecutionContext shardingContext) throws InterruptedException;

	/**
	 * 超时强杀之前调用此方法
	 * @param jobName 作业名
	 * @param shardItem 分片项
	 * @param shardParam 分片参数
	 * @param msgHolders 消息内容
	 * @param shardingContext 其它参数信息
	 */
	public void beforeTimeout(String jobName, Integer shardItem, String shardParam, List<MsgHolder> msgHolders,
			SaturnJobExecutionContext shardingContext) {
		// 由作业类实现逻辑
	}

	/**
	 * 超时强杀之后调用此方法
	 * @param jobName 作业名
	 * @param shardItem 分片项
	 * @param shardParam 分片参数
	 * @param msgHolders 消息内容
	 * @param shardingContext 其它参数信息
	 */
	public void onTimeout(String jobName, Integer shardItem, String shardParam, List<MsgHolder> msgHolders,
			SaturnJobExecutionContext shardingContext) {
		// 由作业类实现逻辑
	}

	/**
	 * 在saturn-console对作业立即终止，或者优雅退出超时，或者与zk失去连接时，都会在强杀业务线程之前调用此方法。
	 * <p>
	 * 注意，作业执行超时，强杀之前不会调用此方法，而是调用{@link #beforeTimeout(String, Integer, String, List, SaturnJobExecutionContext)}方法。
	 * @param jobName 作业名
	 * @param shardItem 分片项
	 * @param shardParam 分片参数
	 * @param msgHolders 消息内容
	 * @param shardingContext 其它参数信息
	 */
	public void beforeForceStop(String jobName, Integer shardItem, String shardParam, List<MsgHolder> msgHolders,
			SaturnJobExecutionContext shardingContext) {
		// 由作业类实现逻辑
	}

	/**
	 * 在saturn-console对作业立即终止，或者优雅退出超时，或者与zk失去连接时，都会在强杀业务线程之后调用此方法。
	 * <p>
	 * 注意，作业执行超时，强杀之后不会调用此方法，而是调用{@link #onTimeout(String, Integer, String, List, SaturnJobExecutionContext)}方法。
	 * @param jobName 作业名
	 * @param shardItem 分片项
	 * @param shardParam 分片参数
	 * @param msgHolders 消息内容
	 * @param shardingContext 其它参数信息
	 */
	public void postForceStop(String jobName, Integer shardItem, String shardParam, List<MsgHolder> msgHolders,
			SaturnJobExecutionContext shardingContext) {
		// 由作业类实现逻辑
	}

}
