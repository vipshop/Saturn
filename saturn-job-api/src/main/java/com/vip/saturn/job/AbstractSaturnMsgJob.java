package com.vip.saturn.job;

import com.vip.saturn.job.msg.MsgHolder;

public abstract class AbstractSaturnMsgJob extends BaseSaturnJob {

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
	 * 超时强杀前调用此方法
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
	 * 立即终止、Restart等造成的<b>强杀之前</b>会调用此方法。
	 * <p>
	 * 特别的是，超时强杀不会调用此方法，而是调用{@link #beforeTimeout(String, Integer, String, MsgHolder, SaturnJobExecutionContext)}方法。
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
	 * 立即终止、Restart等造成的<b>强杀之后</b>会调用此方法。
	 * <p>
	 * 特别的是，超时强杀之后不会调用此方法，而是调用{@link #onTimeout(String, Integer, String, MsgHolder, SaturnJobExecutionContext)}方法。
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

}
