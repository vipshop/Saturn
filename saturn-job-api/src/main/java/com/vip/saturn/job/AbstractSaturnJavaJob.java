package com.vip.saturn.job;

public abstract class AbstractSaturnJavaJob extends AbstractSaturnJob {

	/**
	 * java 作业处理入口
	 *
	 * @param jobName 作业名
	 * @param shardItem 分片ID
	 * @param shardParam 分片参数
	 * @param shardingContext 其它参数信息（预留）
	 */
	public abstract SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) throws InterruptedException;

	public void onTimeout(String jobName, Integer key, String value, SaturnJobExecutionContext shardingContext) {
		// 由作业类实现逻辑
	}

	public void beforeTimeout(String jobName, Integer key, String value, SaturnJobExecutionContext shardingContext) {
		// 由作业类实现逻辑
	}

	public void postForceStop(String jobName, Integer key, String value, SaturnJobExecutionContext shardingContext) {
		// 由作业类实现逻辑
	}

}
