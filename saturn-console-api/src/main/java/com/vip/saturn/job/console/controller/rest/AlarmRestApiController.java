package com.vip.saturn.job.console.controller.rest;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditType;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.service.RestApiService;
import com.vip.saturn.job.console.service.SystemConfigService;
import com.vip.saturn.job.integrate.entity.AlarmInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * RESTful APIs for alarm handling.
 * <p>
 * Created by jeff zhu on 10/05/2017.
 */
@Controller
@RequestMapping("/rest/v1/{namespace}/alarms")
public class AlarmRestApiController extends AbstractRestController {

	public static final String ALARM_TYPE = "SATURN.JOB.EXCEPTION";

	private static final String ALARM_TITLE_EXECUTOR_RESTART = "Executor_Restart";

	private static final String ALARM_NAME_EXECUTOR_RESTART = "Saturn Event";

	private static final String ALARM_RAISED_ON_EXECUTOR_RESTART = "ALARM_RAISED_ON_EXECUTOR_RESTART";

	private static final Logger logger = LoggerFactory.getLogger(AlarmRestApiController.class);

	@Resource
	private RestApiService restApiService;

	@Resource
	private SystemConfigService systemConfigService;

	@Audit(type = AuditType.REST)
	@RequestMapping(value = "/raise", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> raise(@PathVariable("namespace") String namespace,
			@RequestBody Map<String, Object> reqParams, HttpServletRequest request) throws SaturnJobConsoleException {
		try {
			String jobName = checkAndGetParametersValueAsString(reqParams, "jobName", false);
			String executorName = checkAndGetParametersValueAsString(reqParams, "executorName", true);
			Integer shardItem = checkAndGetParametersValueAsInteger(reqParams, "shardItem", false);

			AlarmInfo alarmInfo = constructAlarmInfo(reqParams);

			logger.info("try to raise alarm: {}, job: {}, executor: {}, item: {}", alarmInfo, jobName, executorName,
					shardItem);

			// (since 2.1.4) 如果alarm title是Executor_Restart，而且系统配置ALARM_RAISED_ON_EXECUTOR_RESTART=false, 只记录日志不发送告警
			boolean isExecutorRestartAlarmEvent = isExecutorRestartAlarmEvent(alarmInfo);
			if (isExecutorRestartAlarmEvent) {
				boolean alarmRaisedOnExecutorRestart = systemConfigService
						.getBooleanValue(ALARM_RAISED_ON_EXECUTOR_RESTART, Boolean.FALSE);
				if (!alarmRaisedOnExecutorRestart) {
					logger.warn(alarmInfo.getMessage());
				} else {
					restApiService.raiseExecutorRestartAlarm(namespace, executorName, alarmInfo);
				}
			} else {
				if (StringUtils.isBlank(jobName)) {
					throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
							"Invalid request. Missing parameter: jobName");
				}
				restApiService.raiseAlarm(namespace, jobName, executorName, shardItem, alarmInfo);
			}

			return new ResponseEntity<>(HttpStatus.CREATED);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

	private boolean isExecutorRestartAlarmEvent(AlarmInfo alarmInfo) {
		return ALARM_TITLE_EXECUTOR_RESTART.equals(alarmInfo.getTitle()) && ALARM_NAME_EXECUTOR_RESTART
				.equals(alarmInfo.getName());
	}

	private AlarmInfo constructAlarmInfo(Map<String, Object> reqParams) throws SaturnJobConsoleException {
		AlarmInfo alarmInfo = new AlarmInfo();
		String level = checkAndGetParametersValueAsString(reqParams, "level", true);
		alarmInfo.setLevel(level);

		alarmInfo.setType(ALARM_TYPE);

		String name = checkAndGetParametersValueAsString(reqParams, "name", true);
		alarmInfo.setName(name);

		String title = checkAndGetParametersValueAsString(reqParams, "title", true);
		alarmInfo.setTitle(title);

		String message = checkAndGetParametersValueAsString(reqParams, "message", false);
		if (StringUtils.isNotBlank(message)) {
			alarmInfo.setMessage(message);
		}

		Map<String, String> customFields = (Map<String, String>) reqParams.get("additionalInfo");
		if (customFields != null) {
			alarmInfo.getCustomFields().putAll(customFields);
		}

		return alarmInfo;
	}

}
