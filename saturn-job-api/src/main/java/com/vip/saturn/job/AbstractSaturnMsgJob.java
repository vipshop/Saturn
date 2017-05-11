package com.vip.saturn.job;

import com.vip.saturn.job.alarm.AlarmInfo;
import com.vip.saturn.job.exception.SaturnJobException;
import com.vip.saturn.job.msg.MsgHolder;

import java.util.Map;

public abstract class AbstractSaturnMsgJob {
	private Object saturnApi;
	
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
	public abstract SaturnJobReturn handleMsgJob(String jobName, Integer shardItem, String shardParam, MsgHolder msgHolder,SaturnJobExecutionContext shardingContext) throws InterruptedException;

	public void onTimeout(String jobName, Integer key, String value, MsgHolder msgHolder, SaturnJobExecutionContext shardingContext) {
	}

	public void beforeTimeout(String jobName, Integer key, String value, MsgHolder msgHolder, SaturnJobExecutionContext shardingContext) {
	}
	
	public void postForceStop(String jobName, Integer key, String value, MsgHolder msgHolder, SaturnJobExecutionContext shardingContext) {
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
		if(saturnApi != null){
			Class<?> clazz = saturnApi.getClass();
			try {
				clazz.getMethod("updateJobCron", String.class,String.class,Map.class).invoke(saturnApi, jobName, cron, customContext);
			} catch (Exception e) {
				throw new SaturnJobException(SaturnJobException.SYSTEM_ERROR, e.getMessage(), e.getCause());
			}
		}
	}

	/**
	 * 发告警到Saturn Console。
	 *
	 * @param jobName   作业名。必填。
	 * @param shardItem 分片ID。可以为null。
	 * @param alarmInfo alarm详细信息。
	 */
	public void raiseAlarm(String jobName, Integer shardItem, AlarmInfo alarmInfo) throws SaturnJobException {
		if (saturnApi != null) {
			Class<?> clazz = saturnApi.getClass();
			try {
				clazz.getMethod("raiseAlarm", String.class, Integer.class, AlarmInfo.class).invoke(saturnApi, jobName, shardItem, alarmInfo);
			} catch (Exception e) {
				throw new SaturnJobException(SaturnJobException.SYSTEM_ERROR, e.getMessage(), e.getCause());
			}
		}
	}

	public static Object getObject(){
		return null;
	}
}
