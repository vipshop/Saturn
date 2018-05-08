package com.vip.saturn.job.basic;

import com.vip.saturn.job.exception.SaturnJobException;
import com.vip.saturn.job.utils.AlarmUtils;
import com.vip.saturn.job.utils.UpdateJobCronUtils;
import java.util.Map;

/**
 * Provide the hook for client job callback.
 */
public class SaturnApi {

	private String namespace;

	private String executorName;

	public SaturnApi(String namespace, String executorName) {
		this.namespace = namespace;
		this.executorName = executorName;
	}

	// Make sure that only SaturnApi(String namespace) will be called.
	private SaturnApi() {
	}

	public void updateJobCron(String jobName, String cron, Map<String, String> customContext)
			throws SaturnJobException {
		UpdateJobCronUtils.updateJobCron(namespace, jobName, cron, customContext);
	}

	/**
	 * The hook for client job raise alarm.
	 *
	 * @param alarmInfo The alarm information.
	 */
	public void raiseAlarm(Map<String, Object> alarmInfo) throws SaturnJobException {
		// set executorName into the alarmInfo
		alarmInfo.put("executorName", executorName);
		AlarmUtils.raiseAlarm(alarmInfo, namespace);
	}
}
