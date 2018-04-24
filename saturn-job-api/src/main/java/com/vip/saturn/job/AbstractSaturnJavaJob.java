package com.vip.saturn.job;

public abstract class AbstractSaturnJavaJob extends BaseSaturnJob {

	/**
	 * Java作业处理入口
	 * @param jobName 作业名
	 * @param shardItem 分片项
	 * @param shardParam 分片参数
	 * @param shardingContext 其它参数信息
	 * @return 返回执行结果
	 * @throws InterruptedException 注意处理中断异常
	 */
	public abstract SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) throws InterruptedException;

	/**
	 * 超时强杀之前调用此方法
	 * @param jobName 作业名
	 * @param shardItem 分片项
	 * @param shardParam 分片参数
	 * @param shardingContext 其它参数信息
	 */
	public void beforeTimeout(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) {
		// 由作业类实现逻辑
	}

	/**
	 * 超时强杀之后调用此方法
	 * @param jobName 作业名
	 * @param shardItem 分片项
	 * @param shardParam 分片参数
	 * @param shardingContext 其它参数信息
	 */
	public void onTimeout(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) {
		// 由作业类实现逻辑
	}

	/**
	 * 在saturn-console对作业立即终止，或者优雅退出超时，或者与zk失去连接时，都会在强杀业务线程之前调用此方法。
	 * <p>
	 * 注意，作业执行超时，强杀之前不会调用此方法，而是调用{@link #beforeTimeout(String, Integer, String, SaturnJobExecutionContext)}方法。
	 * @param jobName 作业名
	 * @param shardItem 分片项
	 * @param shardParam 分片参数
	 * @param shardingContext 其它参数信息
	 */
	public void beforeForceStop(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) {
		// 由作业类实现逻辑
	}

	/**
	 * 在saturn-console对作业立即终止，或者优雅退出超时，或者与zk失去连接时，都会在强杀业务线程之后调用此方法。
	 * <p>
	 * 注意，作业执行超时，强杀之后不会调用此方法，而是调用{@link #onTimeout(String, Integer, String, SaturnJobExecutionContext)}方法。
	 * @param jobName 作业名
	 * @param shardItem 分片项
	 * @param shardParam 分片参数
	 * @param shardingContext 其它参数信息
	 */
	public void postForceStop(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) {
		// 由作业类实现逻辑
	}

}
