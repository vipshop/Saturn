package com.vip.saturn.job.console.controller;

import com.alibaba.fastjson.JSON;
import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.domain.RestApiErrorResult;
import com.vip.saturn.job.console.domain.RestApiJobInfo;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.service.JobOperationService;
import com.vip.saturn.job.console.service.RestApiService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @author hebelala
 */
@Controller
@RequestMapping("/rest/v1")
public class RestApiController {

    public final static String BAD_REQ_MSG_PREFIX =  "Invalid request.";

    public final static String INVALID_REQUEST_MSG = BAD_REQ_MSG_PREFIX + " Parameter: {%s} %s";

    public final static String MISSING_REQUEST_MSG = BAD_REQ_MSG_PREFIX + " Missing parameter: {%s}";

    public final static String NOT_EXISTED_PREFIX = "does not exists";

    private final static Logger logger = LoggerFactory.getLogger(RestApiController.class);

    @Resource
    private RestApiService restApiService;

    @Resource
    private JobOperationService jobOperationService;

    @RequestMapping(value = "/{namespace}/job", method = RequestMethod.POST)
    public ResponseEntity<String> createJob(@PathVariable("namespace") String namespace, @RequestBody Map<String,Object> reqParams){
        try{
            JobConfig jobConfig = constructJobConfig(namespace, reqParams);
            jobOperationService.validateJobConfig(jobConfig);

            restApiService.createJob(namespace, jobConfig);

            return new ResponseEntity<String>(HttpStatus.CREATED);
        } catch (Exception e){
            return constructOtherResponses(e);
        }
    }

    @RequestMapping(value = "/{namespace}/{jobName}", method = RequestMethod.GET)
    public ResponseEntity<String> getJobInfo(@PathVariable("namespace") String namespace, @PathVariable("jobName") String jobName){
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        try{
            if(StringUtils.isBlank(namespace)){
                throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), String.format(MISSING_REQUEST_MSG, "namespace"));
            }
            if(StringUtils.isBlank(jobName)){
                throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), String.format(MISSING_REQUEST_MSG, "jobName"));
            }

            RestApiJobInfo restAPIJobInfo = restApiService.getRestAPIJobInfo(namespace, jobName);

            return new ResponseEntity<String>(JSON.toJSONString(restAPIJobInfo), httpHeaders, HttpStatus.OK);
        } catch (Exception e){
            return constructOtherResponses(e);
        }
    }

    @RequestMapping(value = "/{namespace}/jobs", method = RequestMethod.GET)
    public ResponseEntity<String> getJobInfos(@PathVariable("namespace") String namespace, HttpServletRequest request, HttpServletResponse response) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        try {
            if (namespace == null || namespace.trim().length() == 0) {
                throw new SaturnJobConsoleException("The namespace of parameter is required");
            }
            List<RestApiJobInfo> restApiJobInfos = restApiService.getRestApiJobInfos(namespace);
            return new ResponseEntity<String>(JSON.toJSONString(restApiJobInfos), httpHeaders, HttpStatus.OK);
        } catch (Exception e) {
            return constructOtherResponses(e);
        }
    }

    @RequestMapping(value = "/{namespace}/{jobName}/enable", method = RequestMethod.POST)
    public ResponseEntity<String> enableJob(@PathVariable("namespace") String namespace, @PathVariable("jobName") String jobName, HttpServletRequest request, HttpServletResponse response) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        try {
            if (namespace == null || namespace.trim().length() == 0) {
                throw new SaturnJobConsoleException("The namespace of parameter is required");
            }
            if (jobName == null || jobName.trim().length() == 0) {
                throw new SaturnJobConsoleException("The jobName of parameter is required");
            }
            restApiService.enableJob(namespace, jobName);
            return new ResponseEntity<String>(httpHeaders, HttpStatus.OK);
        } catch (Exception e) {
            return constructOtherResponses(e);
        }
    }

    @RequestMapping(value = "/{namespace}/{jobName}/disable", method = RequestMethod.POST)
    public ResponseEntity<String> disableJob(@PathVariable("namespace") String namespace, @PathVariable("jobName") String jobName, HttpServletRequest request, HttpServletResponse response) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        try {
            if (namespace == null || namespace.trim().length() == 0) {
                throw new SaturnJobConsoleException("The namespace of parameter is required");
            }
            if (jobName == null || jobName.trim().length() == 0) {
                throw new SaturnJobConsoleException("The jobName of parameter is required");
            }
            restApiService.disableJob(namespace, jobName);
            return new ResponseEntity<String>(httpHeaders, HttpStatus.OK);
        } catch (Exception e) {
            return constructOtherResponses(e);
        }
    }

    private ResponseEntity<String> constructOtherResponses(Exception e){
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);

        if (e instanceof SaturnJobConsoleHttpException){
            SaturnJobConsoleHttpException saturnJobConsoleHttpException = (SaturnJobConsoleHttpException) e;
            int statusCode = saturnJobConsoleHttpException.getStatusCode();
            switch (statusCode) {
                case 201:
                    return new ResponseEntity<String>(httpHeaders, HttpStatus.CREATED);
                default:
                    return constructErrorResponse(e.getMessage(), HttpStatus.valueOf(statusCode));
            }
        } else if (e instanceof SaturnJobConsoleException){
            if (e.getMessage().contains(NOT_EXISTED_PREFIX)){
                return constructErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
            }
        }

        String message = null;
        if (e.getMessage() == null || e.getMessage().trim().length() == 0) {
            message = e.toString();
        } else {
            message = e.getMessage();
        }

        RestApiErrorResult restApiErrorResult = new RestApiErrorResult();
        restApiErrorResult.setMessage(message);
        return new ResponseEntity<String>(JSON.toJSONString(restApiErrorResult), httpHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<String> constructErrorResponse(String errorMsg, HttpStatus status){
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);

        RestApiErrorResult restApiErrorResult = new RestApiErrorResult();
        restApiErrorResult.setMessage(errorMsg);

        return new ResponseEntity<String>(JSON.toJSONString(restApiErrorResult), httpHeaders, status);
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

        jobConfig.setJobName(checkAndGetParametersValueAsString(reqParams, "jobName", true));

        jobConfig.setDescription(checkAndGetParametersValueAsString(reqParams, "description", false));

        jobConfig.setChannelName(checkAndGetParametersValueAsString(configParams, "channelName", false));

        jobConfig.setCron(checkAndGetParametersValueAsString(configParams, "cron", false));

        jobConfig.setJobClass(checkAndGetParametersValueAsString(configParams, "jobClass", false));

        jobConfig.setJobParameter(checkAndGetParametersValueAsString(configParams, "jobParameter", false));

        jobConfig.setJobType(checkAndGetParametersValueAsString(configParams, "jobType", true));

        jobConfig.setLoadLevel(checkAndGetParametersValueAsInteger(configParams, "loadLevel", false));

        jobConfig.setLocalMode(checkAndGetParametersValueAsBoolean(configParams, "localMode", false));

        jobConfig.setPausePeriodDate(checkAndGetParametersValueAsString(configParams, "pausePeriodDate", false));

        jobConfig.setPausePeriodTime(checkAndGetParametersValueAsString(configParams, "pausePeriodTime", false));

        jobConfig.setPreferList(checkAndGetParametersValueAsString(configParams,"preferList", false));

        jobConfig.setQueueName(checkAndGetParametersValueAsString(configParams,"queueName", false));

        jobConfig.setShardingItemParameters(checkAndGetParametersValueAsString(configParams,"shardingItemParameters", true));

        jobConfig.setShardingTotalCount(checkAndGetParametersValueAsInteger(configParams,"shardingTotalCount", true));

        jobConfig.setTimeout4AlarmSeconds(checkAndGetParametersValueAsInteger(configParams, "timeout4AlarmSeconds", false));

        jobConfig.setTimeoutSeconds(checkAndGetParametersValueAsInteger(configParams, "timeout4Seconds", false));

        jobConfig.setUseDispreferList(checkAndGetParametersValueAsBoolean(configParams, "useDispreferList", false));

        jobConfig.setUseSerial(checkAndGetParametersValueAsBoolean(configParams, "useSerial", false));

        jobConfig.setJobDegree(checkAndGetParametersValueAsInteger(configParams, "jobDegree", false));

        jobConfig.setDependencies(checkAndGetParametersValueAsString(configParams, "dependencies", false));

        return jobConfig;
    }

    private String checkAndGetParametersValueAsString(Map<String, Object> reqParams, String key, boolean isMandatory) throws SaturnJobConsoleException {
        if(reqParams.containsKey(key)) {
            String value =  (String)reqParams.get(key);
            return StringUtils.isBlank(value) ? null : value;
        } else {
            if (isMandatory){
                throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), String.format(MISSING_REQUEST_MSG, key));
            }
            return null;
        }
    }

    private Integer checkAndGetParametersValueAsInteger(Map<String, Object> reqParams, String key, boolean isMandatory) throws SaturnJobConsoleException {
        if(reqParams.containsKey(key)) {
            return (Integer)reqParams.get(key);
        } else {
            if (isMandatory){
                throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), String.format(MISSING_REQUEST_MSG, key));
            }
            return null;
        }
    }

    private Boolean checkAndGetParametersValueAsBoolean(Map<String, Object> reqParams, String key, boolean isMandatory) throws SaturnJobConsoleException {
        if(reqParams.containsKey(key)) {
            return (Boolean)reqParams.get(key);
        } else {
            if (isMandatory){
                throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), String.format(MISSING_REQUEST_MSG, key));
            }
            return Boolean.FALSE;
        }
    }

}
