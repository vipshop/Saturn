package com.vip.saturn.job;

import com.vip.saturn.job.msg.MsgHolder;

public abstract class AbstractSaturnMsgJob extends AbstractSaturnJob {

	/**
	 * vms 作业处理入口
	 *
	 * @param jobName 作业名
	 * @param shardItem 分片ID
	 * @param shardParam 分片参数
	 * @param msgHolder 消息内容
	 * @param shardingContext 其它参数信息（预留）
	 */
	public abstract SaturnJobReturn handleMsgJob(String jobName, Integer shardItem, String shardParam,
			MsgHolder msgHolder, SaturnJobExecutionContext shardingContext) throws InterruptedException;

	public void onTimeout(String jobName, Integer key, String value, MsgHolder msgHolder,
			SaturnJobExecutionContext shardingContext) {
		// 由作业类实现逻辑
	}

	public void beforeTimeout(String jobName, Integer key, String value, MsgHolder msgHolder,
			SaturnJobExecutionContext shardingContext) {
		// 由作业类实现逻辑
	}

	public void postForceStop(String jobName, Integer key, String value, MsgHolder msgHolder,
			SaturnJobExecutionContext shardingContext) {
		// 由作业类实现逻辑
	}

}
