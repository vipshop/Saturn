package com.vip.saturn.job.console.controller;

import com.vip.saturn.job.console.domain.RestApiErrorResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.service.RestApiService;
import com.vip.saturn.job.console.utils.ControllerUtils;
import com.vip.saturn.job.integrate.entity.AlarmInfo;
import org.apache.commons.lang.StringUtils;
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
import java.util.Map;

/**
 * RESTful APIs for alarm handling.
 * <p>
 * Created by kfchu on 10/05/2017.
 */
@Controller
@RequestMapping("/rest/v1/{namespace}/alarms")
public class AlarmRestApiController {

    public final static String NOT_EXISTED_PREFIX = "does not exists";

    public final static String ALARM_TYPE = "SATURN.JOB.EXCEPTION";

    private final static Logger logger = LoggerFactory.getLogger(AlarmRestApiController.class);

    @Resource
    private RestApiService restApiService;

    @RequestMapping(value = "/raise", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Object> raise(@PathVariable("namespace") String namespace, @RequestBody Map<String, Object> reqParams) {
        try {
            String jobName = ControllerUtils.checkAndGetParametersValueAsString(reqParams, "jobName", true);
            String executorName = ControllerUtils.checkAndGetParametersValueAsString(reqParams, "executorName", true);
            Integer shardItem = ControllerUtils.checkAndGetParametersValueAsInteger(reqParams, "shardItem", false);

            AlarmInfo alarmInfo = constructAlarmInfo(reqParams);

            logger.info("try to raise alarm: {}, job: {}, executor: {}, item: {}", alarmInfo.toString(), jobName, executorName, shardItem);

            restApiService.raiseAlarm(namespace, jobName, executorName, shardItem, alarmInfo);

            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (Exception e) {
            return constructOtherResponses(e);
        }
    }

    private AlarmInfo constructAlarmInfo(Map<String, Object> reqParams) throws SaturnJobConsoleException {
        AlarmInfo alarmInfo = new AlarmInfo();
        String level = ControllerUtils.checkAndGetParametersValueAsString(reqParams, "level", true);
        alarmInfo.setLevel(level);

        alarmInfo.setType(ALARM_TYPE);

        String name = ControllerUtils.checkAndGetParametersValueAsString(reqParams, "name", true);
        alarmInfo.setName(name);

        String title = ControllerUtils.checkAndGetParametersValueAsString(reqParams, "title", true);
        alarmInfo.setTitle(title);

        String message = ControllerUtils.checkAndGetParametersValueAsString(reqParams, "message", false);
        if (StringUtils.isNotBlank(message)) {
            alarmInfo.setMessage(message);
        }

        Map<String, String> customFields = (Map<String, String>) reqParams.get("additionalInfo");
        if (customFields != null) {
            alarmInfo.getCustomFields().putAll(customFields);
        }

        return alarmInfo;
    }

    private ResponseEntity<Object> constructOtherResponses(Exception e) {
        HttpHeaders httpHeaders = new HttpHeaders();

        if (e instanceof SaturnJobConsoleHttpException) {
            SaturnJobConsoleHttpException saturnJobConsoleHttpException = (SaturnJobConsoleHttpException) e;
            int statusCode = saturnJobConsoleHttpException.getStatusCode();
            return constructErrorResponse(e.getMessage(), HttpStatus.valueOf(statusCode));
        }

        if (e instanceof SaturnJobConsoleException) {
            if (e.getMessage().contains(NOT_EXISTED_PREFIX)) {
                return constructErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
            }
        }

        String message = null;
        if (e.getMessage() == null || e.getMessage().trim().length() == 0) {
            message = e.toString();
        } else {
            message = e.getMessage();
        }

        RestApiErrorResult restApiErrorResult = new RestApiErrorResult(message);
        return new ResponseEntity<Object>(restApiErrorResult, httpHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Object> constructErrorResponse(String errorMsg, HttpStatus status) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);

        RestApiErrorResult restApiErrorResult = new RestApiErrorResult(errorMsg);
        return new ResponseEntity<Object>(restApiErrorResult, httpHeaders, status);
    }
}
