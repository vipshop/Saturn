package com.vip.saturn.job.console.controller;

import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.RestApiJobInfo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.service.JobOperationService;
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
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * RESTful API of Job Operations.
 *
 * @author hebelala
 */
@Controller
@RequestMapping("/rest/v1")
public class JobOperationRestApiController {

    public final static String BAD_REQ_MSG_PREFIX =  "Invalid request.";

    public final static String INVALID_REQUEST_MSG = BAD_REQ_MSG_PREFIX + " Parameter: {%s} %s";

    public final static String MISSING_REQUEST_MSG = BAD_REQ_MSG_PREFIX + " Missing parameter: {%s}";

    private final static Logger logger = LoggerFactory.getLogger(JobOperationRestApiController.class);

    @Resource
    private RestApiService restApiService;

    @Resource
    private JobOperationService jobOperationService;

    @RequestMapping(value = "/{namespace}/jobs", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Object> create(@PathVariable("namespace") String namespace, @RequestBody Map<String, Object> reqParams) throws SaturnJobConsoleException {
        try{
            JobConfig jobConfig = constructJobConfig(namespace, reqParams);
            jobOperationService.validateJobConfig(jobConfig);

            restApiService.createJob(namespace, jobConfig);

            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (SaturnJobConsoleException e) {
            throw e;
        } catch (Exception e) {
            throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
        }
    }

    @RequestMapping(value = "/{namespace}/jobs/{jobName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Object> query(@PathVariable("namespace") String namespace, @PathVariable("jobName") String jobName) throws SaturnJobConsoleException {
        HttpHeaders httpHeaders = new HttpHeaders();
        try{
            if(StringUtils.isBlank(namespace)){
                throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), String.format(MISSING_REQUEST_MSG, "namespace"));
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
    public ResponseEntity<Object> queryAll(@PathVariable("namespace") String namespace, HttpServletRequest request, HttpServletResponse response) throws SaturnJobConsoleException {
        HttpHeaders httpHeaders = new HttpHeaders();
        try {
            if (StringUtils.isBlank(namespace)) {
                throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), String.format(MISSING_REQUEST_MSG, "namespace"));
            }
            List<RestApiJobInfo> restApiJobInfos = restApiService.getRestApiJobInfos(namespace);
            return new ResponseEntity<Object>(restApiJobInfos, httpHeaders, HttpStatus.OK);
        } catch (SaturnJobConsoleException e) {
            throw e;
        } catch (Exception e) {
            throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
        }
    }

    @RequestMapping(value = {"/{namespace}/{jobName}/enable", "/{namespace}/jobs/{jobName}/enable"}, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Object> enable(@PathVariable("namespace") String namespace, @PathVariable("jobName") String jobName, HttpServletRequest request, HttpServletResponse response) throws SaturnJobConsoleException {
        HttpHeaders httpHeaders = new HttpHeaders();
        try {
            if (StringUtils.isBlank(namespace)) {
                throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), String.format(MISSING_REQUEST_MSG, "namespace"));
            }
            if (StringUtils.isBlank(jobName)) {
                throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), String.format(MISSING_REQUEST_MSG, "jobName"));
            }
            restApiService.enableJob(namespace, jobName);
            return new ResponseEntity<>(httpHeaders, HttpStatus.OK);
        } catch (SaturnJobConsoleException e) {
            throw e;
        } catch (Exception e) {
            throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
        }
    }

    @RequestMapping(value = {"/{namespace}/{jobName}/disable", "/{namespace}/jobs/{jobName}/disable"}, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Object> disable(@PathVariable("namespace") String namespace, @PathVariable("jobName") String jobName, HttpServletRequest request, HttpServletResponse response) throws SaturnJobConsoleException {
        HttpHeaders httpHeaders = new HttpHeaders();
        try {
            if (StringUtils.isBlank(namespace)) {
                throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), String.format(MISSING_REQUEST_MSG, "namespace"));
            }
            if (StringUtils.isBlank(jobName)) {
                throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), String.format(MISSING_REQUEST_MSG, "jobName"));
            }
            restApiService.disableJob(namespace, jobName);
            return new ResponseEntity<>(httpHeaders, HttpStatus.OK);
        } catch (SaturnJobConsoleException e) {
            throw e;
        } catch (Exception e) {
            throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
        }
    }

    private JobConfig constructJobConfig(String namespace, Map<String, Object> reqParams) throws SaturnJobConsoleException {
        JobConfig jobConfig = new JobConfig();

        if(StringUtils.isBlank(namespace)){
            throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), String.format(MISSING_REQUEST_MSG, "namespace"));
        }
        jobConfig.setNamespace(namespace);

        if(!reqParams.containsKey("jobConfig")){
            throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), String.format(INVALID_REQUEST_MSG, "jobConfig", "cannot be blank"));
        }
        Map<String, Object> configParams = (Map<String, Object>) reqParams.get("jobConfig");

        jobConfig.setJobName(ControllerUtils.checkAndGetParametersValueAsString(reqParams, "jobName", true));

        jobConfig.setDescription(ControllerUtils.checkAndGetParametersValueAsString(reqParams, "description", false));

        jobConfig.setChannelName(ControllerUtils.checkAndGetParametersValueAsString(configParams, "channelName", false));

        jobConfig.setCron(ControllerUtils.checkAndGetParametersValueAsString(configParams, "cron", false));

        jobConfig.setJobClass(ControllerUtils.checkAndGetParametersValueAsString(configParams, "jobClass", false));

        jobConfig.setJobParameter(ControllerUtils.checkAndGetParametersValueAsString(configParams, "jobParameter", false));

        jobConfig.setJobType(ControllerUtils.checkAndGetParametersValueAsString(configParams, "jobType", true));

        jobConfig.setLoadLevel(ControllerUtils.checkAndGetParametersValueAsInteger(configParams, "loadLevel", false));

        jobConfig.setLocalMode(ControllerUtils.checkAndGetParametersValueAsBoolean(configParams, "localMode", false));

        jobConfig.setPausePeriodDate(ControllerUtils.checkAndGetParametersValueAsString(configParams, "pausePeriodDate", false));

        jobConfig.setPausePeriodTime(ControllerUtils.checkAndGetParametersValueAsString(configParams, "pausePeriodTime", false));

        jobConfig.setPreferList(ControllerUtils.checkAndGetParametersValueAsString(configParams, "preferList", false));

        jobConfig.setQueueName(ControllerUtils.checkAndGetParametersValueAsString(configParams, "queueName", false));

        jobConfig.setShardingItemParameters(ControllerUtils.checkAndGetParametersValueAsString(configParams, "shardingItemParameters", true));

        jobConfig.setShardingTotalCount(ControllerUtils.checkAndGetParametersValueAsInteger(configParams, "shardingTotalCount", true));

        jobConfig.setTimeout4AlarmSeconds(ControllerUtils.checkAndGetParametersValueAsInteger(configParams, "timeout4AlarmSeconds", false));

        jobConfig.setTimeoutSeconds(ControllerUtils.checkAndGetParametersValueAsInteger(configParams, "timeout4Seconds", false));

        jobConfig.setUseDispreferList(ControllerUtils.checkAndGetParametersValueAsBoolean(configParams, "useDispreferList", false));

        jobConfig.setUseSerial(ControllerUtils.checkAndGetParametersValueAsBoolean(configParams, "useSerial", false));

        jobConfig.setJobDegree(ControllerUtils.checkAndGetParametersValueAsInteger(configParams, "jobDegree", false));

        jobConfig.setDependencies(ControllerUtils.checkAndGetParametersValueAsString(configParams, "dependencies", false));

        jobConfig.setTimeZone(ControllerUtils.checkAndGetParametersValueAsString(configParams, "timeZone", false));

        jobConfig.setTimeoutSeconds(ControllerUtils.checkAndGetParametersValueAsInteger(configParams, "timeoutSeconds", false));

        jobConfig.setProcessCountIntervalSeconds(ControllerUtils.checkAndGetParametersValueAsInteger(configParams, "processCountIntervalSeconds", false));

        jobConfig.setGroups(ControllerUtils.checkAndGetParametersValueAsString(configParams, "groups", false));

        jobConfig.setShowNormalLog(ControllerUtils.checkAndGetParametersValueAsBoolean(configParams, "showNormalLog", false));

        return jobConfig;
    }


}
