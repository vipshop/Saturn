package com.vip.saturn.job.basic;

import java.util.HashMap;
import java.util.Map;

import com.vip.saturn.job.SaturnJobReturn;

/**
 * 分片上下文定义类
 * @author dylan.xue
 */
public class ShardingItemCallable {

	protected final String jobName;

	protected final Integer item;

	protected final String itemValue;

	protected final int timeoutSeconds; // second

	protected final SaturnExecutionContext shardingContext;

	protected final AbstractSaturnJob saturnJob;

	protected SaturnJobReturn saturnJobReturn;

	protected Map<String, String> envMap = new HashMap<>();

	protected boolean businessReturned = false;

	protected long startTime;

	protected long endTime;

	public ShardingItemCallable(String jobName, Integer item, String itemValue, int timeoutSeconds,
			SaturnExecutionContext shardingContext, AbstractSaturnJob saturnJob) {
		super();
		this.jobName = jobName;
		this.item = item;
		this.itemValue = itemValue;
		this.timeoutSeconds = timeoutSeconds;
		this.shardingContext = shardingContext;
		this.saturnJob = saturnJob;
	}

	public long getExecutionTime(){
		return endTime - startTime;
	}

	/**
	 * 获取执行结果对象
	 * @return
	 */
	public SaturnJobReturn getSaturnJobReturn() {
		return saturnJobReturn;
	}

	/**
	 * 设置执行结果
	 * @param saturnJobReturn
	 */
	public void setSaturnJobReturn(SaturnJobReturn saturnJobReturn) {
		this.saturnJobReturn = saturnJobReturn;
	}

	/**
	 * 获取环境变量MAP
	 * @return
	 */
	public Map<String, String> getEnvMap() {
		return envMap;
	}

	/**
	 * 设置环境变量MAP
	 * @param envMap
	 */
	public void setEnvMap(Map<String, String> envMap) {
		this.envMap = envMap;
	}

	/**
	 * 业务代码是否己执行完毕
	 * @return
	 */
	public boolean isBusinessReturned() {
		return businessReturned;
	}

	/**
	 * 设置业务代码是否执行完毕标识，true=己执行完毕；false=未执行完毕
	 * @param businessReturned
	 */
	public void setBusinessReturned(boolean businessReturned) {
		this.businessReturned = businessReturned;
	}

	/**
	 * 获取作业名称
	 * @return
	 */
	public String getJobName() {
		return jobName;
	}

	/**
	 * 获取分片号
	 * @return
	 */
	public Integer getItem() {
		return item;
	}

	/**
	 * 获取分片参数
	 * @return
	 */
	public String getItemValue() {
		return itemValue;
	}

	/**
	 * 获取超时时间
	 * @return
	 */
	public int getTimeoutSeconds() {
		return timeoutSeconds;
	}

	/**
	 * 获取上下文
	 * @return
	 */
	public SaturnExecutionContext getShardingContext() {
		return shardingContext;
	}

	/**
	 * 获取job类实例
	 * @return
	 */
	public AbstractSaturnJob getSaturnJob() {
		return saturnJob;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
}
