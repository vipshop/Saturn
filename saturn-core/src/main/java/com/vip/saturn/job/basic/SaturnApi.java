package com.vip.saturn.job.basic;

import com.alibaba.fastjson.JSONObject;
import com.vip.saturn.job.exception.SaturnJobException;
import com.vip.saturn.job.internal.config.ConfigurationService;
import com.vip.saturn.job.utils.SystemEnvProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Provide the hook for client job callback.
 */
public class SaturnApi {

	private static Logger logger = LoggerFactory.getLogger(SaturnApi.class);

	private static final String SATURN_API_URI_PREFIX = SystemEnvProperties.VIP_SATURN_CONSOLE_URI + "/rest/v1/";

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
		CloseableHttpClient httpClient = null;
		try {
			checkParameters(alarmInfo);
			JSONObject json = new JSONObject(alarmInfo);
			// prepare
			httpClient = HttpClientBuilder.create().build();
			HttpPost request = new HttpPost(SATURN_API_URI_PREFIX + namespace + "/alarm/raise");
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

	private void handleResponse(CloseableHttpResponse httpResponse) throws IOException, SaturnJobException {
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

	private void checkParameters(Map<String, Object> alarmInfo) throws SaturnJobException {
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
			throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, "alarmInfo.name cannot be blank.");
		}

		String title = (String) alarmInfo.get("title");
		if (StringUtils.isBlank(title)) {
			throw new SaturnJobException(SaturnJobException.ILLEGAL_ARGUMENT, "alarmInfo.title cannot be blank.");
		}

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
