package com.vip.saturn.job;

import com.vip.saturn.job.alarm.AlarmInfo;
import com.vip.saturn.job.exception.SaturnJobException;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseSaturnJob {

	private static final String EMPTY_VERSION = "";

	private Object saturnApi;

	public static Object getObject() {
		return null;
	}

	public void setSaturnApi(Object saturnApi) {
		this.saturnApi = saturnApi;
	}

	/**
	 * @return 返回该作业业务的版本号
	 */
	public String getJobVersion() {
		return EMPTY_VERSION;
	}

	/**
	 * 作业启用后，执行该方法。
	 *
	 * @param jobName 作业名
	 */
	public void onEnabled(String jobName) {
	}

	/**
	 * 作业禁用后，执行该方法。
	 *
	 * @param jobName 作业名
	 */
	public void onDisabled(String jobName) {
	}

	/**
	 * 更改作业cron表达式，请确认作业名是正确的。
	 *
	 * @param jobName 作业名
	 * @param cron cron表达式
	 * @param customContext 自定义上下文
	 * @throws SaturnJobException 可能抛的异常有：type为0，表示cron表达式无效；type为1，表示作业名在这个namespace下不存在；type为3，表示customContext内容超出1M。
	 */
	public void updateJobCron(String jobName, String cron, Map<String, String> customContext)
			throws SaturnJobException {
		if (saturnApi != null) {
			Class<?> clazz = saturnApi.getClass();
			try {
				clazz.getMethod("updateJobCron", String.class, String.class, Map.class)
						.invoke(saturnApi, jobName, cron, customContext);
			} catch (Exception e) {
				throw new SaturnJobException(SaturnJobException.SYSTEM_ERROR, e.getMessage(), e);
			}
		}
	}

	/**
	 * 发告警到Saturn Console。
	 *
	 * @param jobName 作业名。必填。
	 * @param shardItem 分片项。可以为null。
	 * @param alarmInfo alarm详细信息。
	 * @throws SaturnJobException 告警异常
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

	private static Map<String, Object> constructMap(String jobName, Integer shardItem, AlarmInfo alarmInfo) {
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
