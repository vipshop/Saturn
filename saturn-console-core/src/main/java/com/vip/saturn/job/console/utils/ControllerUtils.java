package com.vip.saturn.job.console.utils;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Created by kfchu on 10/05/2017.
 */
public class ControllerUtils {

    public final static String BAD_REQ_MSG_PREFIX = "Invalid request.";

    public final static String MISSING_REQUEST_MSG = BAD_REQ_MSG_PREFIX + " Missing parameter: {%s}";

    public static String checkAndGetParametersValueAsString(Map<String, Object> map, String key, boolean isMandatory) throws SaturnJobConsoleException {
        if (map.containsKey(key)) {
            String value = (String) map.get(key);
            return StringUtils.isBlank(value) ? null : value;
        } else {
            if (isMandatory) {
                throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), String.format(MISSING_REQUEST_MSG, key));
            }
            return null;
        }
    }

    public static Integer checkAndGetParametersValueAsInteger(Map<String, Object> map, String key, boolean isMandatory) throws SaturnJobConsoleException {
        if (map.containsKey(key)) {
            return (Integer) map.get(key);
        } else {
            if (isMandatory) {
                throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), String.format(MISSING_REQUEST_MSG, key));
            }
            return null;
        }
    }

    public static Boolean checkAndGetParametersValueAsBoolean(Map<String, Object> map, String key, boolean isMandatory) throws SaturnJobConsoleException {
        if (map.containsKey(key)) {
            return (Boolean) map.get(key);
        } else {
            if (isMandatory) {
                throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), String.format(MISSING_REQUEST_MSG, key));
            }
            return Boolean.FALSE;
        }
    }
}
