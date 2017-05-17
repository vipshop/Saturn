package com.vip.saturn.job.basic;

import com.vip.saturn.job.exception.SaturnJobException;
import com.vip.saturn.job.internal.config.ConfigurationService;
import com.vip.saturn.job.utils.AlarmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Provide the hook for client job callback.
 */
public class SaturnApi {

	private static Logger logger = LoggerFactory.getLogger(SaturnApi.class);

	private String namespace;

	private ConfigurationService configService;

	public SaturnApi(String namespace) {
		this.namespace = namespace;
	}

	// Make sure that only SaturnApi(String namespace) will be called.
	private SaturnApi() {
	}

	public void updateJobCron(String jobName, String cron, Map<String, String> customContext) throws SaturnJobException {
		try {
			configService.updateJobCron(jobName, cron, customContext);
		} catch (SaturnJobException se) {
			logger.error("SaturnJobException throws: {}", se.getMessage());
			throw se;
		} catch (Exception e) {
			logger.error("Other exception throws: {}", e.getMessage());
			throw new SaturnJobException(SaturnJobException.SYSTEM_ERROR, e.getMessage(), e);
		}
	}

	/**
	 * The hook for client job raise alarm.
	 *
	 * @param alarmInfo The alarm information.
	 */
	public void raiseAlarm(Map<String, Object> alarmInfo) throws SaturnJobException {
		AlarmUtils.raiseAlarm(alarmInfo, namespace);
	}


	private SaturnJobException constructSaturnJobException(int statusCode, String msg) {
		if (statusCode >= 400 && statusCode < 500) {
			return new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, msg);
		}
		return new SaturnJobException(SaturnJobException.SYSTEM_ERROR, msg);
	}

	public void setConfigService(ConfigurationService configService) {
		this.configService = configService;
	}
}
