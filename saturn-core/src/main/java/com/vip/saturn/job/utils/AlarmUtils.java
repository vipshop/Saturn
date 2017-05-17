package com.vip.saturn.job.utils;

import com.alibaba.fastjson.JSONObject;
import com.vip.saturn.job.exception.SaturnJobException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Created by jeff.zhu on 17/05/2017.
 */
public class AlarmUtils {

    private static final String SATURN_API_URI_PREFIX = SystemEnvProperties.VIP_SATURN_CONSOLE_URI + "/rest/v1/";

    private final static Logger logger = LoggerFactory.getLogger(AlarmUtils.class);

    /**
     * Raise alarm to Console.
     */
    public static void raiseAlarm(Map<String, Object> alarmInfo, String namespace) throws SaturnJobException {
        logger.info("send alarm of domain {} to console: {}", namespace, alarmInfo.toString());

        String targetUrl = SATURN_API_URI_PREFIX + namespace + "/alarms/raise";

        CloseableHttpClient httpClient = null;
        try {
            checkParameters(alarmInfo);
            JSONObject json = new JSONObject(alarmInfo);
            // prepare
            httpClient = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(targetUrl);
            StringEntity params = new StringEntity(json.toString());
            request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            request.setEntity(params);
            // send request
            CloseableHttpResponse httpResponse = httpClient.execute(request);
            // handle response
            handleResponse(httpResponse);
        } catch (SaturnJobException se) {
            logger.error("SaturnJobException throws: {}", se.getMessage());
            throw se;
        } catch (Exception e) {
            logger.error("Other exception throws: {}", e.getMessage());
            throw new SaturnJobException(SaturnJobException.SYSTEM_ERROR, e.getMessage(), e);
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    logger.error("Exception during httpclient closed.", e);
                }
            }
        }
    }

    private static void handleResponse(CloseableHttpResponse httpResponse) throws IOException, SaturnJobException {
        int status = httpResponse.getStatusLine().getStatusCode();

        if (status == 201) {
            logger.info("raise alarm successfully.");
            return;
        }

        if (status >= 400 && status <= 500) {
            String responseBody = EntityUtils.toString(httpResponse.getEntity());
            if (StringUtils.isNotBlank(responseBody)) {
                String errMsg = JSONObject.parseObject(responseBody).getString("message");
                throw constructSaturnJobException(status, errMsg);
            } else {
                throw new SaturnJobException(SaturnJobException.SYSTEM_ERROR, "internal server error");
            }
        } else {
            // if have unexpected status, then throw RuntimeException directly.
            String errMsg = "unexpected status returned from Saturn Server.";
            throw new SaturnJobException(SaturnJobException.SYSTEM_ERROR, errMsg);
        }
    }

    private static void checkParameters(Map<String, Object> alarmInfo) throws SaturnJobException {
        if (alarmInfo == null) {
            throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, "alarmInfo cannot be null.");
        }

        String jobName = (String) alarmInfo.get("jobName");
        if (StringUtils.isBlank(jobName)) {
            throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, "jobName cannot be blank.");
        }

        String level = (String) alarmInfo.get("level");
        if (StringUtils.isBlank(level)) {
            throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, "level cannot be blank.");
        }

        String name = (String) alarmInfo.get("name");
        if (StringUtils.isBlank(name)) {
            throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, "name cannot be blank.");
        }

        String title = (String) alarmInfo.get("title");
        if (StringUtils.isBlank(title)) {
            throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, "title cannot be blank.");
        }

    }

    private static SaturnJobException constructSaturnJobException(int statusCode, String msg) {
        if (statusCode >= 400 && statusCode < 500) {
            return new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, msg);
        }
        return new SaturnJobException(SaturnJobException.SYSTEM_ERROR, msg);
    }
}
