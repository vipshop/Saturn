package com.vip.saturn.job.console.controller;

import com.vip.saturn.job.console.domain.JobBriefInfo;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.RestApiJobInfo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.service.RestApiService;
import com.vip.saturn.job.console.utils.ControllerUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
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

import java.util.List;
import java.util.Map;

/**
 * RESTful API of Job Operations.
 *
 * @author hebelala
 */
@Controller
@RequestMapping("/rest/v1")
public class JobOperationRestApiController extends AbstractController {

	private static final Logger logger = LoggerFactory.getLogger(JobOperationRestApiController.class);

	@Resource
	private RestApiService restApiService;

	@RequestMapping(value = "/{namespace}/jobs", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> create(@PathVariable("namespace") String namespace,
			@RequestBody Map<String, Object> reqParams) throws SaturnJobConsoleException {
		try {
			JobConfig jobConfig = constructJobConfig(namespace, reqParams);

			restApiService.createJob(namespace, jobConfig);

			return new ResponseEntity<>(HttpStatus.CREATED);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

	@RequestMapping(value = "/{namespace}/jobs/{jobName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> query(@PathVariable("namespace") String namespace,
			@PathVariable("jobName") String jobName) throws SaturnJobConsoleException {
		HttpHeaders httpHeaders = new HttpHeaders();
		try {
			if (StringUtils.isBlank(namespace)) {
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
						String.format(MISSING_REQUEST_MSG, "namespace"));
			}
			if (StringUtils.isBlank(jobName)) {
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
						String.format(MISSING_REQUEST_MSG, "jobName"));
			}

			RestApiJobInfo restAPIJobInfo = restApiService.getRestAPIJobInfo(namespace, jobName);

			return new ResponseEntity<Object>(restAPIJobInfo, httpHeaders, HttpStatus.OK);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

	@RequestMapping(value = "/{namespace}/jobs", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> queryAll(@PathVariable("namespace") String namespace)
			throws SaturnJobConsoleException {
		HttpHeaders httpHeaders = new HttpHeaders();
		try {
			if (StringUtils.isBlank(namespace)) {
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
						String.format(MISSING_REQUEST_MSG, "namespace"));
			}
			List<RestApiJobInfo> restApiJobInfos = restApiService.getRestApiJobInfos(namespace);
			return new ResponseEntity<Object>(restApiJobInfos, httpHeaders, HttpStatus.OK);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

	@RequestMapping(value = { "/{namespace}/{jobName}/enable",
			"/{namespace}/jobs/{jobName}/enable" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> enable(@PathVariable("namespace") String namespace,
			@PathVariable("jobName") String jobName) throws SaturnJobConsoleException {
		HttpHeaders httpHeaders = new HttpHeaders();
		try {
			if (StringUtils.isBlank(namespace)) {
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
						String.format(MISSING_REQUEST_MSG, "namespace"));
			}
			if (StringUtils.isBlank(jobName)) {
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
						String.format(MISSING_REQUEST_MSG, "jobName"));
			}

			restApiService.enableJob(namespace, jobName);
			return new ResponseEntity<>(httpHeaders, HttpStatus.OK);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

	@RequestMapping(value = { "/{namespace}/{jobName}/disable",
			"/{namespace}/jobs/{jobName}/disable" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> disable(@PathVariable("namespace") String namespace,
			@PathVariable("jobName") String jobName) throws SaturnJobConsoleException {
		HttpHeaders httpHeaders = new HttpHeaders();
		try {
			if (StringUtils.isBlank(namespace)) {
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
						String.format(MISSING_REQUEST_MSG, "namespace"));
			}
			if (StringUtils.isBlank(jobName)) {
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
						String.format(MISSING_REQUEST_MSG, "jobName"));
			}

			restApiService.disableJob(namespace, jobName);
			return new ResponseEntity<>(httpHeaders, HttpStatus.OK);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

	@RequestMapping(value = { "/{namespace}/{jobName}/cron",
			"/{namespace}/jobs/{jobName}/cron" }, method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> updateJobCron(@PathVariable("namespace") String namespace,
			@PathVariable("jobName") String jobName, @RequestBody Map<String, String> params,
			HttpServletRequest request) throws SaturnJobConsoleException {
		HttpHeaders httpHeaders = new HttpHeaders();
		try {
			if (StringUtils.isBlank(namespace)) {
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
						String.format(MISSING_REQUEST_MSG, "namespace"));
			}
			if (StringUtils.isBlank(jobName)) {
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
						String.format(MISSING_REQUEST_MSG, "jobName"));
			}
			String cron = params.remove("cron");
			if (StringUtils.isBlank(cron)) {
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
						String.format(MISSING_REQUEST_MSG, "cron"));
			}

			restApiService.updateJobCron(namespace, jobName, cron, params);
			return new ResponseEntity<>(httpHeaders, HttpStatus.OK);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

	@RequestMapping(value = "/{namespace}/jobs/{jobName}/run", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> run(@PathVariable("namespace") String namespace,
			@PathVariable("jobName") String jobName) throws SaturnJobConsoleException {
		try {
			if (StringUtils.isBlank(namespace)) {
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
						String.format(MISSING_REQUEST_MSG, "namespace"));
			}
			if (StringUtils.isBlank(jobName)) {
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
						String.format(MISSING_REQUEST_MSG, "jobName"));
			}

			restApiService.runJobAtOnce(namespace, jobName);
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

	@RequestMapping(value = "/{namespace}/jobs/{jobName}/stop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> stop(@PathVariable("namespace") String namespace,
			@PathVariable("jobName") String jobName) throws SaturnJobConsoleException {
		try {
			if (StringUtils.isBlank(namespace)) {
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
						String.format(MISSING_REQUEST_MSG, "namespace"));
			}
			if (StringUtils.isBlank(jobName)) {
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
						String.format(MISSING_REQUEST_MSG, "jobName"));
			}

			restApiService.stopJobAtOnce(namespace, jobName);

			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

	@RequestMapping(value = "/{namespace}/jobs/{jobName}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> delete(@PathVariable("namespace") String namespace,
			@PathVariable("jobName") String jobName) throws SaturnJobConsoleException {
		try {
			if (StringUtils.isBlank(namespace)) {
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
						String.format(MISSING_REQUEST_MSG, "namespace"));
			}
			if (StringUtils.isBlank(jobName)) {
				throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
						String.format(MISSING_REQUEST_MSG, "jobName"));
			}

			restApiService.deleteJob(namespace, jobName);
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Exception e) {
			throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
		}
	}

	private JobConfig constructJobConfig(String namespace, Map<String, Object> reqParams)
			throws SaturnJobConsoleException {
		JobConfig jobConfig = new JobConfig();

		if (StringUtils.isBlank(namespace)) {
			throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
					String.format(MISSING_REQUEST_MSG, "namespace"));
		}
		jobConfig.setNamespace(namespace);

		if (!reqParams.containsKey("jobConfig")) {
			throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
					String.format(INVALID_REQUEST_MSG, "jobConfig", "cannot be blank"));
		}
		Map<String, Object> configParams = (Map<String, Object>) reqParams.get("jobConfig");

		jobConfig.setJobName(ControllerUtils.checkAndGetParametersValueAsString(reqParams, "jobName", true));

		jobConfig.setDescription(ControllerUtils.checkAndGetParametersValueAsString(reqParams, "description", false));

		jobConfig
				.setChannelName(ControllerUtils.checkAndGetParametersValueAsString(configParams, "channelName", false));

		jobConfig.setCron(ControllerUtils.checkAndGetParametersValueAsString(configParams, "cron", false));

		jobConfig.setJobClass(ControllerUtils.checkAndGetParametersValueAsString(configParams, "jobClass", false));

		jobConfig.setJobParameter(
				ControllerUtils.checkAndGetParametersValueAsString(configParams, "jobParameter", false));

		String jobType = ControllerUtils.checkAndGetParametersValueAsString(configParams, "jobType", true);
		if (JobBriefInfo.JobType.UNKOWN_JOB.equals(JobBriefInfo.JobType.getJobType(jobType))) {
			throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(),
					String.format(INVALID_REQUEST_MSG, "jobType", "is malformed"));
		}
		jobConfig.setJobType(jobType);

		jobConfig.setLoadLevel(ControllerUtils.checkAndGetParametersValueAsInteger(configParams, "loadLevel", false));

		jobConfig.setLocalMode(ControllerUtils.checkAndGetParametersValueAsBoolean(configParams, "localMode", false));

		jobConfig.setPausePeriodDate(
				ControllerUtils.checkAndGetParametersValueAsString(configParams, "pausePeriodDate", false));

		jobConfig.setPausePeriodTime(
				ControllerUtils.checkAndGetParametersValueAsString(configParams, "pausePeriodTime", false));

		jobConfig.setPreferList(ControllerUtils.checkAndGetParametersValueAsString(configParams, "preferList", false));

		jobConfig.setQueueName(ControllerUtils.checkAndGetParametersValueAsString(configParams, "queueName", false));

		jobConfig.setShardingItemParameters(
				ControllerUtils.checkAndGetParametersValueAsString(configParams, "shardingItemParameters", true));

		jobConfig.setShardingTotalCount(
				ControllerUtils.checkAndGetParametersValueAsInteger(configParams, "shardingTotalCount", true));

		jobConfig.setTimeout4AlarmSeconds(
				ControllerUtils.checkAndGetParametersValueAsInteger(configParams, "timeout4AlarmSeconds", false));

		jobConfig.setTimeoutSeconds(
				ControllerUtils.checkAndGetParametersValueAsInteger(configParams, "timeout4Seconds", false));

		jobConfig.setUseDispreferList(
				ControllerUtils.checkAndGetParametersValueAsBoolean(configParams, "useDispreferList", false));

		jobConfig.setUseSerial(ControllerUtils.checkAndGetParametersValueAsBoolean(configParams, "useSerial", false));

		jobConfig.setJobDegree(ControllerUtils.checkAndGetParametersValueAsInteger(configParams, "jobDegree", false));

		jobConfig.setDependencies(
				ControllerUtils.checkAndGetParametersValueAsString(configParams, "dependencies", false));

		jobConfig.setTimeZone(ControllerUtils.checkAndGetParametersValueAsString(configParams, "timeZone", false));

		jobConfig.setTimeoutSeconds(
				ControllerUtils.checkAndGetParametersValueAsInteger(configParams, "timeoutSeconds", false));

		jobConfig.setProcessCountIntervalSeconds(ControllerUtils.checkAndGetParametersValueAsInteger(configParams,
				"processCountIntervalSeconds", false));

		jobConfig.setGroups(ControllerUtils.checkAndGetParametersValueAsString(configParams, "groups", false));

		jobConfig.setShowNormalLog(
				ControllerUtils.checkAndGetParametersValueAsBoolean(configParams, "showNormalLog", false));

		return jobConfig;
	}

}
