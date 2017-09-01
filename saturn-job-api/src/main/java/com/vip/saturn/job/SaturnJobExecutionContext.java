package com.vip.saturn.job;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class SaturnJobExecutionContext implements Serializable {
	private static final long serialVersionUID = -5213585560266060611L;

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
	 * 获取到的本片
	 */
	private List<Integer> shardingItems;

	/**
	 * 运行在本作业项的分片序列号和个性化参数列表.
	 */
	private Map<Integer, String> shardingItemParameters;

	/**
	 * 自定义上下文
	 */
	private Map<String, String> customContext;

	public SaturnJobExecutionContext() {

	}

	public void copyFrom(Object source) {
		Class<?> clazz = source.getClass();
		try {
			Field field = null;
			Object res = null;

			field = clazz.getDeclaredField("jobName");
			field.setAccessible(true);
			res = field.get(source);
			if (res != null) {
				this.jobName = (String) res;
			}

			field = clazz.getDeclaredField("shardingTotalCount");
			field.setAccessible(true);
			res = field.get(source);
			if (res != null) {
				this.shardingTotalCount = (int) res;
			}

			field = clazz.getDeclaredField("jobParameter");
			field.setAccessible(true);
			res = field.get(source);
			if (res != null) {
				this.jobParameter = (String) res;
			}

			field = clazz.getDeclaredField("shardingItems");
			field.setAccessible(true);
			res = field.get(source);
			if (res != null) {
				this.shardingItems = (List) res;
			}

			field = clazz.getDeclaredField("shardingItemParameters");
			field.setAccessible(true);
			res = field.get(source);
			if (res != null) {
				this.shardingItemParameters = (Map) res;
			}

			field = clazz.getDeclaredField("customContext");
			field.setAccessible(true);
			res = field.get(source);
			if (res != null) {
				this.customContext = (Map) res;
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

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

	public List<Integer> getShardingItems() {
		return shardingItems;
	}

	public void setShardingItems(List<Integer> shardingItems) {
		this.shardingItems = shardingItems;
	}

	public Map<Integer, String> getShardingItemParameters() {
		return shardingItemParameters;
	}

	public void setShardingItemParameters(Map<Integer, String> shardingItemParameters) {
		this.shardingItemParameters = shardingItemParameters;
	}

	public Map<String, String> getCustomContext() {
		return customContext;
	}

	public void setCustomContext(Map<String, String> customContext) {
		this.customContext = customContext;
	}

}
