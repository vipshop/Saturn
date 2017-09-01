package com.vip.saturn.job;

import com.vip.saturn.job.alarm.AlarmInfo;
import com.vip.saturn.job.exception.SaturnJobException;
import com.vip.saturn.job.msg.MsgHolder;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractSaturnMsgJob {
	private static final String EMPTY_VERSION = "";

	private Object saturnApi;

	public static Object getObject() {
		return null;
	}

	public void setSaturnApi(Object saturnApi) {
		this.saturnApi = saturnApi;
	}

	/**
	 * vms 作业处理入口
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
	}

	public void beforeTimeout(String jobName, Integer key, String value, MsgHolder msgHolder,
			SaturnJobExecutionContext shardingContext) {
	}

	public void postForceStop(String jobName, Integer key, String value, MsgHolder msgHolder,
			SaturnJobExecutionContext shardingContext) {
	}

	/**
	 * @return version of the job
	 */
	public String getJobVersion() { // NOSONAR
		return EMPTY_VERSION;
	}

	/**
	 * The job was just enabled.
	 */
	public void onEnabled(String jobName) {
	}

	/**
	 * The job was just disabled.
	 */
	public void onDisabled(String jobName) {
	}

	public void updateJobCron(String jobName, String cron, Map<String, String> customContext) throws Exception {
		if (saturnApi != null) {
			Class<?> clazz = saturnApi.getClass();
			try {
				clazz.getMethod("updateJobCron", String.class, String.class, Map.class).invoke(saturnApi, jobName, cron,
						customContext);
			} catch (Exception e) {
				throw new SaturnJobException(SaturnJobException.SYSTEM_ERROR, e.getMessage(), e);
			}
		}
	}

	/**
	 * 发告警到Saturn Console。
	 *
	 * @param jobName 作业名。必填。
	 * @param shardItem 分片ID。可以为null。
	 * @param alarmInfo alarm详细信息。
	 */
	public void raiseAlarm(String jobName, Integer shardItem, AlarmInfo alarmInfo) throws SaturnJobException {
		if (saturnApi != null) {
			Class<?> clazz = saturnApi.getClass();
			try {
				clazz.getMethod("raiseAlarm", Map.class).invoke(saturnApi, constructMap(jobName, shardItem, alarmInfo));
			} catch (Exception e) {
				throw new SaturnJobException(SaturnJobException.SYSTEM_ERROR, e.getMessage(), e);
			}
		}
	}

	private Map<String, Object> constructMap(String jobName, Integer shardItem, AlarmInfo alarmInfo) {
		Map<String, Object> ret = new HashMap<>();

		ret.put("jobName", jobName);
		ret.put("shardItem", shardItem);
		ret.put("name", alarmInfo.getName());
		ret.put("title", alarmInfo.getTitle());
		ret.put("level", alarmInfo.getLevel());
		ret.put("message", alarmInfo.getMessage());
		ret.put("additionalInfo", alarmInfo.getCustomFields());

		return ret;
	}

}
