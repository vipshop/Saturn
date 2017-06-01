package com.vip.saturn.job.utils;

import com.alibaba.fastjson.JSONObject;
import com.vip.saturn.job.exception.SaturnJobException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
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
 * Util for handling alarm.
 * <p>
 * Created by jeff.zhu on 17/05/2017.
 */
public class AlarmUtils {

    private static final String SATURN_API_URI_PREFIX = SystemEnvProperties.VIP_SATURN_CONSOLE_URI + "/rest/v1/";

    private final static Logger logger = LoggerFactory.getLogger(AlarmUtils.class);

    /**
     * Send alarm request to Alarm API in Console.
     */
    public static void raiseAlarm(Map<String, Object> alarmInfo, String namespace) throws SaturnJobException {
        String targetUrl = SATURN_API_URI_PREFIX + namespace + "/alarms/raise";

        logger.info("raise alarm of domain {} to url {}: {}", namespace, targetUrl, alarmInfo.toString());

        CloseableHttpClient httpClient = null;
        try {
            checkParameters(alarmInfo);
            JSONObject json = new JSONObject(alarmInfo);
            // prepare
            httpClient = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(targetUrl);
            final RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(10000).build();
            request.setConfig(requestConfig);
            StringEntity params = new StringEntity(json.toString());
            request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            request.setEntity(params);

            // send request
            CloseableHttpResponse httpResponse = httpClient.execute(request);
            // handle response
            handleResponse(httpResponse);
        } catch (SaturnJobException se) {
            logger.error("SaturnJobException throws: {}", se);
            throw se;
        } catch (Exception e) {
            logger.error("Other exception throws: {}", e);
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

        if (status == HttpStatus.SC_CREATED) {
            logger.info("raise alarm successfully.");
            return;
        }

        if (status >= HttpStatus.SC_BAD_REQUEST && status <= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            String responseBody = EntityUtils.toString(httpResponse.getEntity());
            if (StringUtils.isNotBlank(responseBody)) {
                String errMsg = JSONObject.parseObject(responseBody).getString("message");
                throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, errMsg);
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

}
